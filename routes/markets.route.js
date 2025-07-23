const express = require("express");
const router = express.Router();
const neo4j = require("neo4j-driver");
const { v4: uuidv4 } = require("uuid"); // 🆕 הוספה: ייבוא ספריית UUID

const driver = neo4j.driver(
  "bolt://localhost:7687", // כתובת בסיס הנתונים המקומי
  neo4j.auth.basic("neo4j", "loolrov17")
);

const session = driver.session();

// ה-endpoint הקיים שלך
router.get("/", async (req, res) => {
  try {
    const result = await session.run("MATCH (m:Market) RETURN m");
    const markets = result.records.map((record) => record.get("m").properties);
    res.json(markets);
  } catch (error) {
    console.error(error);
    res.status(500).send("Error fetching users");
  }
});

// POST - הוספת שוק חדש (עם ID ייחודי והחזרת ID)
router.post("/addMarket", async (req, res) => {
  console.log("=== POST /addMarket ===");
  console.log("Request body:", req.body);

  const { date, latitude, location, longitude, farmerEmail } = req.body;

  if (
    !date ||
    !location ||
    latitude == null ||
    longitude == null ||
    !farmerEmail
  ) {
    return res.status(400).json({
      message: "Missing required fields",
      required: ["date", "location", "latitude", "longitude", "farmerEmail"],
      received: { date, location, latitude, longitude, farmerEmail },
    });
  }

  try {
    // 🆕 צור ID ייחודי לשוק
    const marketId = uuidv4();

    // בדיקה אם שוק עם אותו תאריך ומיקום כבר קיים (אופציונלי, אם אתה רוצה לאפשר רק שוק אחד ליום במיקום נתון)
    const checkResult = await session.run(
      "MATCH (m:Market {date: $date, location: $location}) RETURN m",
      { date, location }
    );

    if (checkResult.records.length > 0) {
      return res
        .status(409)
        .json({ message: "Market already exists at this date and location" });
    }

    // יצירת השוק עם ה-ID החדש
    const createMarketResult = await session.run(
      "CREATE (m:Market {id: $marketId, date: $date, latitude: $latitude, location: $location, longitude: $longitude}) RETURN m",
      {
        marketId, // 🆕 הוספנו את ה-ID
        date,
        latitude: parseFloat(latitude),
        location,
        longitude: parseFloat(longitude),
      }
    );

    const marketProperties = createMarketResult.records[0].get("m").properties;

    // יצירת קשר FOUNDER
    await session.run(
      `MATCH (f:Person {email: $email}), (m:Market {id: $marketId}) // 🆕 השתמש ב-marketId
            CREATE (f)-[:FOUNDER]->(m)`,
      { email: farmerEmail, marketId } // 🆕 השתמש ב-marketId
    );

    console.log("Market and FOUNDER relation created:", marketProperties);

    res.status(201).json({
      message: "Market created and linked to farmer",
      marketId: marketId, // 🆕 החזר את ה-ID של השוק
      market: marketProperties,
    });
  } catch (error) {
    console.error("Error adding market:", error);
    res.status(500).json({
      message: "Internal server error",
      error: error.message,
    });
  }
});

// קובץ הראוטר שלך (או קובץ השרת הראשי)

// זה ה-endpoint שצריך לטפל בבקשת הפרופיל
router.get("/profile", async (req, res) => {
  const { location, date } = req.query;
  if (!location || !date) {
    return res.status(400).send("Location and date are required.");
  }
  try {
    const result = await session.run(
      `MATCH (m:Market {location: $location, date: $date})
      OPTIONAL MATCH (founder:Person)-[:FOUNDER]->(m)
      OPTIONAL MATCH (m)-[r:INVITE {participate: true}]->(invitedFarmer:Person) 
      RETURN m.name AS name, 
              m.hours AS hours, 
              COLLECT(DISTINCT invitedFarmer.name) AS invitedAndParticipatingFarmers,
              founder.email AS founderEmail,
              founder.name AS founderName,
              m.id AS marketId
      `,
      { location, date }
    );

    if (result.records.length === 0) {
      return res.status(404).json({
        message: "Market not found with the specified location and date.",
      });
    }
    const record = result.records[0];

    // איסוף כל השמות של החקלאים המוזמנים והמשתתפים
    const allOtherFarmers = new Set();
    record.get("invitedAndParticipatingFarmers").forEach((name) => {
      if (name) allOtherFarmers.add(name);
    });

    const marketData = {
      name: record.get("name") || location,
      hours: record.get("hours") || "09:00 - 14:00",
      founderName: record.get("founderName"),
      founderEmail: record.get("founderEmail"),
      otherFarmers: Array.from(allOtherFarmers),
      marketId: record.get("marketId"),
    };
    res.json(marketData);
  } catch (error) {
    console.error("Error fetching market profile:", error);
    res.status(500).send("Error fetching market profile data.");
  }
});

router.get("/farmer-markets/:email", async (req, res) => {
  const { email } = req.params; // 🎯 קבלת המייל מהפרמטרים של ה-URL

  if (!email) {
    return res.status(400).send("Farmer email is required.");
  }

  try {
    const result = await session.run(
      `MATCH (f:Person {email: $email}) 
        OPTIONAL MATCH (f)-[r:INVITE]->(m:Market) 
        WHERE r.participate = true 

      OPTIONAL MATCH (f)-[:FOUNDER]->(m_founder:Market) 

       // אסוף את הפרטים הרלוונטיים משני סוגי השווקים
        WITH f, COLLECT(DISTINCT {
          id: m.id,
          location: m.location,
          date: m.date
        }) AS invitedAndParticipatingMarkets,
        COLLECT(DISTINCT {
          id: m_founder.id,
          location: m_founder.location,
          date: m_founder.date
        }) AS foundedMarkets

       // שלב את הרשימות וודא שווקים ייחודיים
       // אנחנו משלבים ולא אכפת לנו מ-participated כאן כי המטרה היא רשימת השווקים שהחקלאי פעיל בהם
        UNWIND invitedAndParticipatingMarkets + foundedMarkets AS allMarketData
        RETURN DISTINCT allMarketData.id AS marketId,
                        allMarketData.location AS location,
                        allMarketData.date AS date
        ORDER BY date(allMarketData.date) ASC 
      `,
      { email }
    );

    if (result.records.length === 0) {
      return res.json([]);
    }

    const farmerParticipatingMarkets = result.records.map((record) => ({
      marketId: record.get("marketId"),
      location: record.get("location"),
      date: record.get("date"),
    }));

    res.json(farmerParticipatingMarkets);
  } catch (error) {
    console.error("Error fetching farmer's participating markets:", error);
    res.status(500).json({
      message: "Error fetching farmer's participating markets data.",
      error: error.message,
    });
  }
});

// זה ה-endpoint עבור רשימת כל השווקים/תאריכים
router.get("/locations-dates", async (req, res) => {
  try {
    const result = await session.run(
      "MATCH (m:Market) RETURN m.location AS location, m.date AS date"
    );
    const markets = result.records.map((record) => ({
      location: record.get("location"),
      date: record.get("date"),
    }));
    res.json(markets); // <--- מחזיר מערך
  } catch (error) {
    console.error("Error fetching market locations and dates:", error);
    res.status(500).send("Error fetching market data");
  }
});

// POST - הזמנת חקלאי לשוק (כעת מקבל marketId)
router.post("/inviteFarmer", async (req, res) => {
  const { marketId, invitedEmail, inviterEmail } = req.body; // 🆕 שינוי: מקבל marketId

  if (!marketId || !invitedEmail || !inviterEmail) {
    return res.status(400).json({
      message: "Missing required fields: marketId, invitedEmail, inviterEmail",
    });
  }

  try {
    // 1. ודא שהחקלאי המזמין (inviter) קיים
    const inviterResult = await session.run(
      `MATCH (inviter:Person {email: $inviterEmail}) RETURN inviter`,
      { inviterEmail }
    );
    if (inviterResult.records.length === 0) {
      return res.status(404).json({ message: "Inviter (founder) not found." });
    }

    // 2. ודא שהחקלאי המוזמן (invited) קיים
    const invitedResult = await session.run(
      `MATCH (invited:Person {email: $invitedEmail}) RETURN invited`,
      { invitedEmail }
    );
    if (invitedResult.records.length === 0) {
      return res.status(404).json({ message: "Invited farmer not found." });
    }

    // 3. ודא שהשוק קיים
    const marketResult = await session.run(
      `MATCH (market:Market {id: $marketId}) RETURN market`, // 🆕 השתמש ב-marketId
      { marketId }
    );
    if (marketResult.records.length === 0) {
      return res.status(404).json({ message: "Market not found." });
    }

    // 4. צור או עדכן קשר INVITE עם participate=false
    await session.run(
      `MATCH (market:Market {id: $marketId}), (farmer:Person {email: $invitedEmail})
            MERGE (market)-[r:INVITE]->(farmer)
            ON CREATE SET r.participate = false
            ON MATCH SET r.participate = false`,
      { marketId, invitedEmail } // 🆕 השתמש ב-marketId
    );

    res.status(200).json({ message: "Invitation sent successfully." });
  } catch (error) {
    console.error("Error inviting farmer:", error);
    res.status(500).json({ message: "Server error", error: error.message });
  }
});

// 🆕 GET - חיפוש חקלאים לפי שם או אימייל
router.get("/searchFarmers", async (req, res) => {
  const { query } = req.query; // קבל את שאילתת החיפוש מה-query parameters

  if (!query || query.trim() === "") {
    return res.status(400).json({ message: "Search query is required" });
  }

  try {
    const result = await session.run(
      `MATCH (p:Person)
            WHERE toLower(p.name) CONTAINS toLower($query) OR toLower(p.email) CONTAINS toLower($query)
            RETURN p.name AS name, p.email AS email`,
      { query: query }
    );

    const farmers = result.records.map((record) => ({
      name: record.get("name"),
      email: record.get("email"),
    }));

    res.status(200).json({ farmers: farmers });
  } catch (error) {
    console.error("Error searching farmers:", error);
    res.status(500).json({ message: "Server error", error: error.message });
  }
});

// קבלת כל ההזמנות של משתמש לפי אימייל (ייתכן שתצטרך להתאים אם תעבור ל-marketId)
router.get("/invitations/:email", async (req, res) => {
  const { email } = req.params;

  try {
    const result = await session.run(
      `MATCH (m:Market)-[r:INVITE {participate: false}]->(f:Person {email: $email})
             RETURN m.id AS marketId, m.date AS date, m.location AS location`,
      { email }
    );

    const invitations = result.records.map((record) => ({
      marketId: record.get("marketId"),
      date: record.get("date"),
      location: record.get("location"),
    }));

    res.status(200).json({ invitations });
  } catch (error) {
    console.error("Error fetching invitations:", error);
    res.status(500).json({ message: "Server error", error: error.message });
  }
});

// PUT - קבלת הזמנה (ייתכן שתצטרך להתאים אם תעבור ל-marketId)
// קובץ הראוטר שלך (לדוגמה, marketsRouter.js)

router.put("/acceptInvitation", async (req, res) => {
  const { email, marketId } = req.body;
  console.log("Incoming request for acceptInvitation. Body:", req.body);
  try {
    const result = await session.run(
      `MATCH (m:Market {id: $marketId})-[r:INVITE]->(f:Person {email: $email}) 
       SET r.participate = true
       RETURN r`,
      { email, marketId } // ה-email וה-marketId מגיעים לכאן
    );

    if (result.records.length > 0) {
      res
        .status(200)
        .json({ success: true, message: "Invitation accepted successfully." });
    } else {
      res.status(404).json({
        success: false,
        message: "Invitation not found or already accepted.",
      });
    }
  } catch (error) {
    console.error("Error accepting invitation:", error);
    res.status(500).json({
      success: false,
      message: "Server error during acceptance",
      error: error.message,
    });
  }
});
router.delete("/declineInvitation", async (req, res) => {
  const { email, marketId } = req.body;

  try {
    const result = await session.run(
      `MATCH (m:Market {id: $marketId})-[r:INVITE]->(f:Person {email: $email})
       DELETE r
       RETURN r`,
      { email, marketId }
    );

    if (result.records.length === 0) {
      res.status(404).json({
        success: false,
        message: "Invitation not found or already declined.",
      });
    } else {
      res
        .status(200)
        .json({ success: true, message: "Invitation declined successfully" });
    }
  } catch (error) {
    console.error("Error declining invitation:", error);
    res
      .status(500)
      .json({ success: false, message: "Server error", error: error.message });
  }
});

module.exports = router;
