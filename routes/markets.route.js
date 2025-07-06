const express = require("express");
const router = express.Router();
neo4j = require("neo4j-driver");

const driver = neo4j.driver(
  "bolt://localhost:7687", // כתובת בסיס הנתונים המקומי
  neo4j.auth.basic("neo4j", "315833301") // שים את הסיסמה שלך
);

const session = driver.session();
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

// POST - הוספת שוק חדש
router.post("/addMarket", async (req, res) => {
  console.log("=== POST /addMarket ===");
  console.log("Request body:", req.body);

  const { date, latitude, location, longitude, farmerEmail } = req.body;

  if (!date || !location || latitude == null || longitude == null || !farmerEmail) {
    return res.status(400).json({
      message: "Missing required fields",
      required: ["date", "location", "latitude", "longitude", "farmerEmail"],
      received: { date, location, latitude, longitude, farmerEmail },
    });
  }

  try {
    // בדיקה אם השוק כבר קיים
    const checkResult = await session.run(
      "MATCH (m:Market {date: $date, location: $location}) RETURN m",
      { date, location }
    );

    if (checkResult.records.length > 0) {
      return res.status(409).json({ message: "Market already exists" });
    }

    // יצירת השוק
    const createMarketResult = await session.run(
      "CREATE (m:Market {date: $date, latitude: $latitude, location: $location, longitude: $longitude}) RETURN m",
      {
        date,
        latitude: parseFloat(latitude),
        location,
        longitude: parseFloat(longitude),
      }
    );

    const market = createMarketResult.records[0].get("m").properties;

    // יצירת קשר FOUNDER
    await session.run(
      `MATCH (f:Person {email: $email}), (m:Market {date: $date, location: $location})
       CREATE (f)-[:FOUNDER]->(m)`,
      { email: farmerEmail, date, location }
    );

    console.log("Market and FOUNDER relation created:", market);

    res.status(201).json({
      message: "Market created and linked to farmer",
      market: market,
    });

  } catch (error) {
    console.error("Error adding market:", error);
    res.status(500).json({
      message: "Internal server error",
      error: error.message,
    });
  }
});


router.get("/locations-dates", async (req, res) => {
  try {
    const result = await session.run("MATCH (m:Market) RETURN m.location AS location, m.date AS date");
    
    const markets = result.records.map(record => ({
      location: record.get("location"),
      date: record.get("date"),
    }));

    res.json(markets);
  } catch (error) {
    console.error("Error fetching market locations and dates:", error);
    res.status(500).send("Error fetching market data");
  }
});


// POST - הזמנת חקלאי לשוק
router.post("/inviteFarmer", async (req, res) => {
  const { marketDate, marketLocation, invitedEmail, inviterEmail } = req.body;

  if (!marketDate || !marketLocation || !invitedEmail || !inviterEmail) {
    return res.status(400).json({ message: "Missing required fields" });
  }

  try {
    // ודא שהשוק והחקלאים קיימים
    await session.run(`MERGE (f:Person {email: $inviterEmail})`, { inviterEmail });
    await session.run(`MERGE (f:Person {email: $invitedEmail})`, { invitedEmail });

    // צור קשר INVITE עם status
    await session.run(
      `MATCH (farmer:Person {email: $invitedEmail}), (market:Market {date: $marketDate, location: $marketLocation})
        MERGE (market)-[r:INVITE]->(farmer)
        SET r.participate = false`,
      { invitedEmail, marketDate, marketLocation }
    );

    res.status(200).json({ message: "Invitation sent" });

  } catch (error) {
    console.error("Error inviting farmer:", error);
    res.status(500).json({ message: "Server error", error: error.message });
  }
});


// קבלת כל ההזמנות של משתמש לפי אימייל
router.get("/invitations/:email", async (req, res) => {
  const { email } = req.params;

  try {
    const result = await session.run(
      `MATCH (m:Market)-[r:INVITE {participate: false}]->(f:Person {email: $email})
       RETURN m.date AS date, m.location AS location`,
      { email }
    );

    const invitations = result.records.map(record => ({
      date: record.get("date"),
      location: record.get("location"),
    }));

    res.status(200).json({ invitations });

  } catch (error) {
    console.error("Error fetching invitations:", error);
    res.status(500).json({ message: "Server error", error: error.message });
  }
});

router.put("/acceptInvitation", async (req, res) => {
  const { email, marketDate, marketLocation } = req.body;

  try {
    // עדכן את ההזמנה ל-participate=true
    await session.run(
      `MATCH (m:Market {date: $marketDate, location: $marketLocation})-[r:INVITE]->(f:Person {email: $email})
       SET r.participate = true`,
      { email, marketDate, marketLocation }
    );

    // צור קשר participate
    await session.run(
      `MATCH (f:Person {email: $email}), (m:Market {date: $marketDate, location: $marketLocation})
       MERGE (f)-[:PARTICIPATE]->(m)`,
      { email, marketDate, marketLocation }
    );

    res.status(200).json({ message: "Invitation accepted" });

  } catch (error) {
    console.error("Error accepting invitation:", error);
    res.status(500).json({ message: "Server error", error: error.message });
  }
});



module.exports = router;
