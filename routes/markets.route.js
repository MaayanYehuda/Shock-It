const express = require("express");
const router = express.Router();
const neo4j = require("neo4j-driver");
const { v4: uuidv4 } = require("uuid");

const driver = neo4j.driver(
  "bolt://localhost:7687",
  neo4j.auth.basic("neo4j", "loolrov17")
  //  neo4j.auth.basic("neo4j", "315833301")
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

router.get("/order", async (req, res) => {
  const { userLat, userLon, currentDate } = req.query;
  console.log("Incoming request to /markets/order with params:", {
    userLat,
    userLon,
    currentDate,
  });

  let session;
  try {
    session = driver.session();

    let queryParts = [`MATCH (m:Market)`];
    let params = {};
    let orderByClauses = [];

    //list all properties from the start.
    let returnItems = [
      `m.id AS id`,
      `m.date AS date`,
      `m.hours AS hours`,
      `m.location AS location`,
      `m.latitude AS latitude`,
      `m.longitude AS longitude`,
    ];

    if (currentDate) {
      queryParts.push(`WHERE m.date >= $currentDate`);
      params.currentDate = currentDate;
      orderByClauses.push(`m.date ASC`);
    }

    if (userLat && userLon) {
      queryParts.push(`
        WITH m,
        point({latitude: toFloat(m.latitude), longitude: toFloat(m.longitude)}) AS marketPoint,
        point({latitude: toFloat($userLat), longitude: toFloat($userLon)}) AS userPoint
      `);
      queryParts.push(`
        WITH m, point.distance(userPoint, marketPoint) AS distance
      `);
      returnItems.push(`distance AS distance`);
      orderByClauses.push(`distance ASC`);
      params.userLat = userLat;
      params.userLon = userLon;
    }

    let finalQuery = queryParts.join(` `);

    if (orderByClauses.length > 0) {
      finalQuery += ` ORDER BY ` + orderByClauses.join(", ");
    }

    finalQuery += ` RETURN ` + returnItems.join(", ");

    console.log("Cypher Query:", finalQuery);
    console.log("Cypher Params:", params);

    const result = await session.run(finalQuery, params);
    const markets = result.records.map((record) => {
      // get the properties directly from the record using the aliases
      const hours = record.get("hours") || "09:00 - 16:00";
      const distance = record.has("distance") ? record.get("distance") : null;

      return {
        id: record.get("id"),
        date: record.get("date"),
        hours: hours,
        location: record.get("location"),
        latitude: record.get("latitude"),
        longitude: record.get("longitude"),
        distance: distance,
      };
    });

    console.log("Fetched markets (before sending to client):", markets);
    res.json(markets);
  } catch (error) {
    console.error("Error fetching markets:", error);
    res.status(500).json({
      success: false,
      message: "Error fetching markets.",
      details: error.message,
      stack: error.stack,
    });
  } finally {
    if (session) {
      session.close();
    }
  }
});

router.post("/addMarket", async (req, res) => {
  console.log("=== POST /addMarket ===");
  console.log("Request body:", req.body);
  const { date, hours, latitude, location, longitude, farmerEmail } = req.body;

  if (
    !date ||
    !hours ||
    !location ||
    latitude == null ||
    longitude == null ||
    !farmerEmail
  ) {
    return res.status(400).json({
      message: "Missing required fields",
      required: [
        "date",
        "hours",
        "location",
        "latitude",
        "longitude",
        "farmerEmail",
      ],
      received: { date, hours, location, latitude, longitude, farmerEmail },
    });
  }

  try {
    const marketId = uuidv4();

    // בדיקה אם שוק עם אותו תאריך ומיקום כבר קיים
    const checkResult = await session.run(
      "MATCH (m:Market {date: $date, location: $location})  RETURN m",
      { date, location }
    );

    if (checkResult.records.length > 0) {
      return res
        .status(409)
        .json({ message: "Market already exists at this date and location" });
    }

    const createMarketResult = await session.run(
      "CREATE (m:Market {id: $marketId, date: $date,hours: $hours , latitude: $latitude, location: $location, longitude: $longitude}) RETURN m",
      {
        marketId,
        date,
        hours,
        latitude: parseFloat(latitude),
        location,
        longitude: parseFloat(longitude),
      }
    );

    const marketProperties = createMarketResult.records[0].get("m").properties;

    // יצירת קשר FOUNDER
    await session.run(
      `MATCH (f:Person {email: $email}), (m:Market {id: $marketId}) 
            CREATE (f)-[:FOUNDER]->(m)`,
      { email: farmerEmail, marketId }
    );

    console.log("Market and FOUNDER relation created:", marketProperties);

    res.status(201).json({
      message: "Market created and linked to farmer",
      marketId: marketId,
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

router.get("/profile", async (req, res) => {
  const { location, date } = req.query;

  if (!location || !date) {
    return res.status(400).send("מיקום ותאריך נדרשים.");
  }

  const session = driver.session();
  try {
    const result = await session.run(
      `
      MATCH (m:Market {location: $location, date: $date})
      OPTIONAL MATCH (f_founder:Person)-[:FOUNDER]->(m)

      // אוסף את פרטי החקלאים המשתתפים (אלו שהוזמנו והשתתפותם אושרה)
      OPTIONAL MATCH (p_participant:Person)<-[invited:INVITE]-(m)
      WHERE invited.participate = true
      WITH m, f_founder, COLLECT(DISTINCT {
          name: p_participant.name,
          email: p_participant.email
      }) AS participatingFarmers

      // אוסף את פרטי החקלאים שהוזמנו אך טרם אישרו
      OPTIONAL MATCH (p_invited:Person)<-[invited:INVITE]-(m)
      WHERE invited.participate = false
      WITH m, f_founder, participatingFarmers, COLLECT(DISTINCT {
          name: p_invited.name,
          email: p_invited.email
      }) AS invitedFarmers

      // אוסף את פרטי החקלאים עם בקשות הצטרפות ממתינות
      OPTIONAL MATCH (p_pending:Person)-[:REQUEST]->(m)
      WITH m, f_founder, participatingFarmers, invitedFarmers, COLLECT(DISTINCT {
          name: p_pending.name,
          email: p_pending.email
      }) AS pendingRequests

      // אוסף את מוצרי השוק הספציפיים
      OPTIONAL MATCH (m)-[will_be:WILL_BE]->(marketItem:Item)<-[offers_item:OFFERS]-(farmerOfferingMarketItem:Person)
      WITH m, f_founder, participatingFarmers, invitedFarmers, pendingRequests, COLLECT(DISTINCT {
          name: marketItem.name,
          description: marketItem.description,
          price: will_be.marketPrice,
          offeringFarmerName: farmerOfferingMarketItem.name,
          offeringFarmerEmail: farmerOfferingMarketItem.email
      }) AS marketProducts

      RETURN {
          id: m.id,
          name: m.name,
          location: m.location,
          date: m.date,
          hours: m.hours,
          latitude: m.latitude,
          longitude: m.longitude,
          founderName: f_founder.name,
          founderEmail: f_founder.email,
          participatingFarmers: participatingFarmers,
          invitedFarmers: invitedFarmers,
          pendingRequests: pendingRequests,
          marketProducts: marketProducts
      } AS marketProfile
      `,
      { location, date }
    );

    if (result.records.length === 0) {
      return res.status(404).send("השוק לא נמצא.");
    }

    const marketProfile = result.records[0].get("marketProfile");
    marketProfile.hours = marketProfile.hours || "09:00 - 16:00";

    const cleanArray = (arr) =>
      arr.length === 1 && arr[0].name === null
        ? []
        : arr.filter((item) => item.name !== null && item.email !== null);

    marketProfile.participatingFarmers = cleanArray(
      marketProfile.participatingFarmers
    );
    marketProfile.invitedFarmers = cleanArray(marketProfile.invitedFarmers);
    marketProfile.pendingRequests = cleanArray(marketProfile.pendingRequests);

    // ניקוי המערך marketProducts
    if (
      marketProfile.marketProducts.length === 1 &&
      marketProfile.marketProducts[0].name === null
    ) {
      marketProfile.marketProducts = [];
    }

    res.json(marketProfile);
  } catch (error) {
    console.error("שגיאה באחזור פרופיל השוק:", error);
    res.status(500).send("שגיאת שרת פנימית: " + error.message);
  } finally {
    session.close();
  }
});

// POST - הזמנת חקלאי לשוק
router.post("/inviteFarmer", async (req, res) => {
  const { marketId, invitedEmail, inviterEmail } = req.body;

  if (!marketId || !invitedEmail || !inviterEmail) {
    return res.status(400).json({
      message: "Missing required fields: marketId, invitedEmail, inviterEmail",
    });
  }

  try {
    const inviterResult = await session.run(
      `MATCH (inviter:Person {email: $inviterEmail}) RETURN inviter`,
      { inviterEmail }
    );
    if (inviterResult.records.length === 0) {
      return res.status(404).json({ message: "Inviter (founder) not found." });
    }

    const invitedResult = await session.run(
      `MATCH (invited:Person {email: $invitedEmail}) RETURN invited`,
      { invitedEmail }
    );
    if (invitedResult.records.length === 0) {
      return res.status(404).json({ message: "Invited farmer not found." });
    }

    const marketResult = await session.run(
      `MATCH (market:Market {id: $marketId}) RETURN market`,
      { marketId }
    );
    if (marketResult.records.length === 0) {
      return res.status(404).json({ message: "Market not found." });
    }

    await session.run(
      `MATCH (market:Market {id: $marketId}), (farmer:Person {email: $invitedEmail})
            MERGE (market)-[r:INVITE]->(farmer)
            ON CREATE SET r.participate = false
            ON MATCH SET r.participate = false`,
      { marketId, invitedEmail }
    );

    res.status(200).json({ message: "Invitation sent successfully." });
  } catch (error) {
    console.error("Error inviting farmer:", error);
    res.status(500).json({ message: "Server error", error: error.message });
  }
});

// חיפוש חקלאים לפי שם או אימייל
router.get("/searchFarmers", async (req, res) => {
  const { query } = req.query;

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

// אישור הזמנה לשוק
router.put("/acceptInvitation", async (req, res) => {
  const { email, marketId } = req.body;
  console.log("Incoming request for acceptInvitation. Body:", req.body);
  try {
    const result = await session.run(
      `MATCH (m:Market {id: $marketId})-[r:INVITE]->(f:Person {email: $email}) 
       SET r.participate = true
       RETURN r`,
      { email, marketId }
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

router.post("/:marketId/add-product", async (req, res) => {
  const { marketId } = req.params;
  const { farmerEmail, itemName, price } = req.body;

  if (!marketId || !farmerEmail || !itemName || price == null) {
    return res.status(400).json({
      success: false,
      message: "Market ID, farmer email, item name, and price are required.",
    });
  }

  let session;
  try {
    session = driver.session();
    const result = await session.run(
      `
      MATCH (m:Market {id: $marketId})
      MATCH (f:Person {email: $farmerEmail})-[offers:OFFERS]->(item:Item {name: $itemName})

      WHERE item.price IS NOT NULL
      
      MERGE (m)-[wb:WILL_BE]->(item)
      SET wb.marketPrice = $price

      RETURN m, item, wb
      `,
      { marketId, farmerEmail, itemName, price: parseFloat(price) }
    );

    if (result.records.length === 0) {
      return res.status(404).json({
        success: false,
        message:
          "Could not add product. Market, farmer, or item not found, or item not offered by farmer.",
      });
    }

    console.log(
      `Product '${itemName}' added to market '${marketId}' by '${farmerEmail}' successfully.`
    );
    res.status(200).json({
      success: true,
      message: "Product successfully added/updated in market.",
      marketId: marketId,
      itemName: itemName,
    });
  } catch (error) {
    console.error("Error adding product to market:", error);
    res.status(500).json({
      success: false,
      message: "Error adding product to market.",
      details: error.message,
    });
  } finally {
    if (session) {
      session.close();
    }
  }
});

// לקבלת כל השווקים שהחקלאי משתתף בהם (כמייסד או כחלק מהשתתפות מאושרת)
router.get("/farmer-markets/:email", async (req, res) => {
  const { email } = req.params;

  if (!email) {
    return res.status(400).send("Farmer email is required.");
  }

  const session = driver.session();
  try {
    const result = await session.run(
      `
      MATCH (f:Person {email: $email})
      OPTIONAL MATCH (f)-[:FOUNDER]->(m_founded:Market)

      OPTIONAL MATCH (f)<-[r:INVITE]-(m_invited:Market)
      WHERE r.participate = true

      WITH f, COLLECT(DISTINCT {
          id: m_founded.id,
          location: m_founded.location,
          date: m_founded.date
      }) AS foundedMarkets,
      COLLECT(DISTINCT {
          id: m_invited.id,
          location: m_invited.location,
          date: m_invited.date
      }) AS invitedMarkets

      // איחוד וסינון כפילויות
      UNWIND foundedMarkets + invitedMarkets AS allMarketData
      WITH DISTINCT allMarketData
      WHERE allMarketData.id IS NOT NULL 
      RETURN allMarketData.id AS marketId,
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
  } finally {
    session.close();
  }
});

router.post("/:marketId/request", async (req, res) => {
  const { marketId } = req.params;
  const { email, products } = req.body;
  const farmerEmail = email;
  console.log("Incoming request to join market:", {
    marketId,
    farmerEmail,
    products,
  });
  if (
    !marketId ||
    !farmerEmail ||
    !Array.isArray(products) ||
    products.length === 0
  ) {
    console.log(
      "Missing required fields - this should not happen if data is sent correctly."
    );
    return res.status(400).json({
      success: false,
      message: "מזהה שוק, מייל חקלאי ורשימת מוצרים נדרשים.",
    });
  }

  let session;
  try {
    session = driver.session();
    console.log("Neo4j session created. Running initial checks...");

    const checkResult = await session.run(
      `
      MATCH (m:Market {id: $marketId})
      MATCH (f:Person {email: $farmerEmail})
      RETURN m, f
      `,
      { marketId, farmerEmail }
    );

    if (checkResult.records.length === 0) {
      return res
        .status(404)
        .json({ success: false, message: "השוק או החקלאי לא נמצאו." });
    }
    console.log("Market and farmer found, proceeding with request...");

    const existingRequest = await session.run(
      `
        MATCH (f:Person {email: $farmerEmail})-[:REQUEST]->(m:Market {id: $marketId})
        RETURN count(m) AS count
        `,
      { marketId, farmerEmail }
    );
    console.log("Existing request check result:", existingRequest.records);

    if (existingRequest.records[0].get("count").toInt() > 0) {
      return res.status(409).json({
        success: false,
        message: "בקשת הצטרפות כבר קיימת עבור שוק זה.",
      });
    }

    const tx = session.beginTransaction();

    try {
      await tx.run(
        `
        MATCH (f:Person {email: $farmerEmail})
        MATCH (m:Market {id: $marketId})
        MERGE (f)-[r:REQUEST]->(m)
        SET r.requestedProducts = $productsJson
        `,
        { farmerEmail, marketId, productsJson: JSON.stringify(products) }
      );
      await tx.commit();
      console.log("Request sent successfully with products:", products);
      res.status(200).json({ success: true, message: "הבקשה נשלחה בהצלחה." });
    } catch (txError) {
      console.error("Transaction failed, rolling back:", txError);
      await tx.rollback();
      res.status(500).json({
        success: false,
        message: "שגיאה בשליחת הבקשה: " + txError.message,
      });
    }
  } catch (error) {
    console.error("שגיאה בשליחת בקשת הצטרפות:", error);
    res
      .status(500)
      .json({ success: false, message: "שגיאת שרת פנימית: " + error.message });
  } finally {
    if (session) {
      session.close();
    }
  }
});

router.get("/:marketId/requests", async (req, res) => {
  const { marketId } = req.params;

  if (!marketId) {
    return res.status(400).send("מזהה שוק נדרש.");
  }

  let session;
  try {
    session = driver.session();
    console.log(`Fetching pending requests for market ID: ${marketId}`);

    const result = await session.run(
      `
  MATCH (f:Person)-[r:REQUEST]->(m:Market {id: $marketId})
  WHERE r.status IS NULL OR r.status <> "declined"
  RETURN f.name AS farmerName, 
         f.email AS farmerEmail, 
         r.status AS status, 
         r.requestedProducts AS requestedProducts
  `,
      { marketId }
    );

    const requests = result.records.map((record) => {
      const requestedProductsJson = record.get("requestedProducts");
      let products = [];
      if (requestedProductsJson && !requestedProductsJson.status) {
        try {
          products = JSON.parse(requestedProductsJson);
        } catch (e) {
          console.error(
            "Error parsing requestedProducts JSON for farmer:",
            record.get("farmerEmail"),
            e
          );
        }
      }
      return {
        farmerName: record.get("farmerName"),
        farmerEmail: record.get("farmerEmail"),
        requestedProducts: products,
      };
    });

    res.status(200).json(requests);
  } catch (error) {
    console.error("שגיאה באחזור בקשות הצטרפות:", error);
    res.status(500).send("שגיאת שרת פנימית: " + error.message);
  } finally {
    if (session) {
      session.close();
    }
  }
});
// POST - אישור בקשת הצטרפות לשוק
router.post("/:marketId/requests/approve", async (req, res) => {
  const { marketId } = req.params;
  const { farmerEmail } = req.body;

  if (!marketId || !farmerEmail) {
    return res.status(400).send("מזהה שוק ומייל חקלאי נדרשים לאישור.");
  }

  let session;
  try {
    session = driver.session();
    console.log(
      `Attempting to approve request for farmer ${farmerEmail} in market ${marketId}`
    );

    const requestResult = await session.run(
      `
      MATCH (f:Person {email: $farmerEmail})-[r:REQUEST]->(m:Market {id: $marketId})
      RETURN r.requestedProducts AS requestedProducts, f, m
      `,
      { farmerEmail, marketId }
    );

    if (requestResult.records.length === 0) {
      return res.status(404).send("בקשת הצטרפות לא נמצאה.");
    }

    const requestedProductsJson =
      requestResult.records[0].get("requestedProducts");

    let productsToApprove = [];
    if (requestedProductsJson) {
      try {
        productsToApprove = JSON.parse(requestedProductsJson);
      } catch (e) {
        console.error(
          "Error parsing requestedProducts JSON during approval:",
          e
        );
        return res.status(500).send("שגיאה פנימית: פורמט מוצרים שגוי בבקשה.");
      }
    }

    const tx = session.beginTransaction();

    try {
      await tx.run(
        `
        MATCH (f:Person {email: $farmerEmail})-[r:REQUEST]->(m:Market {id: $marketId})
        DELETE r
        `,
        { farmerEmail, marketId }
      );

      await tx.run(
        `
        MATCH (f:Person {email: $farmerEmail})
        MATCH (m:Market {id: $marketId})
        MERGE (m)-[i:INVITE]->(f)
        SET i.participate = true
        `,
        { farmerEmail, marketId }
      );

      for (const product of productsToApprove) {
        console.log(
          `--- Processing product: ${product.name} for farmer: ${farmerEmail} ---`
        );
        console.log(
          `Attempting to MATCH Item with name: '${product.name}' offered by farmer: '${farmerEmail}'`
        );

        const itemMatchResult = await tx.run(
          `
          MATCH (f:Person {email: $farmerEmail})-[:OFFERS]->(i:Item {name: $productName})
          RETURN i
          `,
          {
            farmerEmail: farmerEmail,
            productName: product.name,
          }
        );

        const itemNode = itemMatchResult.records[0]
          ? itemMatchResult.records[0].get("i")
          : null;

        if (itemNode) {
          console.log(
            `SUCCESS: Found existing Item node for '${product.name}'. Item properties:`,
            itemNode.properties
          );
          await tx.run(
            `
            MATCH (m:Market {id: $marketId})
            MATCH (i:Item {name: $productName}) // גם כאן, הסר ownerEmail
            // ודא ש-i קיים בטווח השאילתה הזו, ושהוא מקושר לחקלאי הנכון
            // (ה-MATCH הקודם כבר אימת את זה, אבל עדיף לוודא שוב אם זו שאילתה נפרדת)
            MATCH (f:Person {email: $farmerEmail})-[:OFFERS]->(i) 
            MERGE (m)-[wb:WILL_BE]->(i)
            ON CREATE SET wb.marketPrice = $marketPrice
            ON MATCH SET wb.marketPrice = $marketPrice
            `,
            {
              marketId: marketId,
              farmerEmail: farmerEmail,
              productName: product.name,
              marketPrice: product.price,
            }
          );
          console.log(
            `SUCCESS: WILL_BE relationship processed for product: ${product.name}.`
          );
        } else {
          console.warn(
            `WARNING: Item '${product.name}' offered by '${farmerEmail}' NOT FOUND via OFFERS relationship. WILL_BE relationship NOT created for this product. Check if the product exists or if 'OFFERS' relationship is correct.`
          );
        }
      }

      await tx.commit();
      console.log(
        `Request for farmer ${farmerEmail} in market ${marketId} approved successfully.`
      );
      res
        .status(200)
        .json({ success: true, message: "בקשת ההצטרפות אושרה בהצלחה." });
    } catch (txError) {
      console.error(
        "Transaction failed during approval, rolling back:",
        txError
      );
      await tx.rollback();
      res.status(500).send("שגיאה באישור הבקשה: " + txError.message);
    }
  } catch (error) {
    console.error("שגיאה בשליחת בקשת אישור הצטרפות:", error);
    res.status(500).send("שגיאת שרת פנימית: " + error.message);
  } finally {
    if (session) {
      session.close();
    }
  }
});

// PUT - דחיית בקשת הצטרפות לשוק (עדכון סטטוס בקשה קיימת)
router.put("/:marketId/requests/decline", async (req, res) => {
  const { marketId } = req.params;
  const { farmerEmail } = req.body;

  if (!marketId || !farmerEmail) {
    return res.status(400).send("מזהה שוק ומייל חקלאי נדרשים לדחייה.");
  }

  let session;
  try {
    session = driver.session();
    console.log(
      `Attempting to decline request for farmer ${farmerEmail} in market ${marketId}`
    );

    const tx = session.beginTransaction();

    try {
      const result = await tx.run(
        `
        MATCH (f:Person {email: $farmerEmail})-[r:REQUEST]->(m:Market {id: $marketId})
        SET r.status = 'declined'  
        RETURN r
        `,
        { farmerEmail, marketId }
      );

      if (result.records.length === 0) {
        await tx.rollback();
        return res.status(404).send("בקשת הצטרפות לא נמצאה או כבר נדחתה.");
      }

      await tx.commit();
      console.log(
        `Request for farmer ${farmerEmail} in market ${marketId} declined successfully (status updated).`
      );
      res
        .status(200)
        .json({ success: true, message: "בקשת ההצטרפות נדחתה בהצלחה." });
    } catch (txError) {
      console.error(
        "Transaction failed during decline, rolling back:",
        txError
      );
      await tx.rollback();
      res.status(500).send("שגיאה בדחיית הבקשה: " + txError.message);
    }
  } catch (error) {
    console.error("שגיאה בשליחת בקשת דחיית הצטרפות:", error);
    res.status(500).send("שגיאת שרת פנימית: " + error.message);
  } finally {
    if (session) {
      session.close();
    }
  }
});

router.get("/search", async (req, res) => {
  const { query, userLat, userLon } = req.query;

  console.log("--- Incoming Search Request ---");
  console.log("Raw Query Params:", req.query);

  if (
    !query ||
    typeof query !== "string" ||
    query.trim() === "" ||
    !userLat ||
    !userLon
  ) {
    console.error(
      "Validation failed: Missing or invalid search query or location."
    );
    return res.status(400).json({
      message: "Search query and user location (userLat, userLon) are required",
    });
  }

  try {
    const userLatFloat = parseFloat(userLat);
    const userLonFloat = parseFloat(userLon);

    if (isNaN(userLatFloat) || isNaN(userLonFloat)) {
      console.error(
        `Validation failed: Failed to parse userLat (${userLat}) or userLon (${userLon}).`
      );
      return res
        .status(400)
        .json({ message: "Invalid user location provided." });
    }

    console.log("Parsed User Location:", {
      latitude: userLatFloat,
      longitude: userLonFloat,
    });

    const allResults = {
      markets: [],
      farmers: [],
    };
    const uniqueIdentifiers = new Set();

    // This query finds markets and calculates distance.
    const marketLocationQuery = `
            MATCH (m:Market)
            WHERE toLower(m.location) CONTAINS toLower($query) OR toLower(m.date) CONTAINS toLower($query)
            WITH m, point({latitude: toFloat(m.latitude), longitude: toFloat(m.longitude)}) AS marketPoint
            RETURN m AS market, point.distance(marketPoint, point({latitude: $userLat, longitude: $userLon})) AS distance
        `;
    const marketResult = await session.run(marketLocationQuery, {
      query: query,
      userLat: userLatFloat,
      userLon: userLonFloat,
    });

    for (const record of marketResult.records) {
      const market = record.get("market").properties;
      const identifier = `Market-${market.id}`;
      if (!uniqueIdentifiers.has(identifier)) {
        allResults.markets.push({
          type: "Market",
          ...market,
          distance: record.get("distance"),
        });
        uniqueIdentifiers.add(identifier);
      }
    }

    // This query finds farmers and includes all their participating markets.
    const farmerQuery = `
            MATCH (p:Person)
            WHERE toLower(p.name) CONTAINS toLower($query) OR toLower(p.email) CONTAINS toLower($query)
            OPTIONAL MATCH (p)<-[:INVITE {participate: true}]-(m:Market)
            WITH p, collect(m) as markets, point({latitude: toFloat(p.lat), longitude: toFloat(p.lon)}) AS farmerPoint
            RETURN p AS farmer, markets, point.distance(farmerPoint, point({latitude: $userLat, longitude: $userLon})) AS distance
        `;
    const farmerResult = await session.run(farmerQuery, {
      query: query,
      userLat: userLatFloat,
      userLon: userLonFloat,
    });

    for (const record of farmerResult.records) {
      const farmer = record.get("farmer").properties;
      const identifier = `Farmer-${farmer.email}`;
      if (!uniqueIdentifiers.has(identifier)) {
        allResults.farmers.push({
          type: "Farmer",
          ...farmer,
          distance: record.get("distance"),
          participatingMarkets: record
            .get("markets")
            .map((market) => market.properties),
        });
        uniqueIdentifiers.add(identifier);
      }
    }

    // This query searches for items and returns both the farmers and markets associated with them.
    const productQuery = `
            // Find farmers offering the product
            MATCH (p:Person)-[:OFFERS]->(i:Item)
            WHERE toLower(i.name) CONTAINS toLower($query) OR toLower(i.description) CONTAINS toLower($query)
            OPTIONAL MATCH (p)-[:INVITE {participate: true}]->(m:Market)
            WITH DISTINCT p, collect(m) AS markets, point({latitude: toFloat(p.lat), longitude: toFloat(p.lon)}) AS farmerPoint
            RETURN 'Farmer' AS type, p AS entity, markets, point.distance(farmerPoint, point({latitude: $userLat, longitude: $userLon})) AS distance
            
            UNION ALL
            
            // Find markets with the product
            MATCH (i:Item)<-[:WILL_BE]-(m:Market)
            WHERE toLower(i.name) CONTAINS toLower($query) OR toLower(i.description) CONTAINS toLower($query)
            WITH DISTINCT m, point({latitude: toFloat(m.latitude), longitude: toFloat(m.longitude)}) AS marketPoint
            RETURN 'Market' AS type, m AS entity, null as markets, point.distance(marketPoint, point({latitude: $userLat, longitude: $userLon})) AS distance
        `;
    const productResult = await session.run(productQuery, {
      query: query,
      userLat: userLatFloat,
      userLon: userLonFloat,
    });

    for (const record of productResult.records) {
      const type = record.get("type");
      const entity = record.get("entity").properties;
      const distance = record.get("distance");

      if (type === "Farmer") {
        const identifier = `Farmer-${entity.email}`;
        if (!uniqueIdentifiers.has(identifier)) {
          allResults.farmers.push({
            type: "Farmer",
            ...entity,
            distance: distance,
            participatingMarkets: record
              .get("markets")
              .map((market) => market.properties),
          });
          uniqueIdentifiers.add(identifier);
        }
      } else if (type === "Market") {
        const identifier = `Market-${entity.id}`;
        if (!uniqueIdentifiers.has(identifier)) {
          allResults.markets.push({
            type: "Market",
            ...entity,
            distance: distance,
          });
          uniqueIdentifiers.add(identifier);
        }
      }
    }

    // Sort markets by date, then by distance
    allResults.markets.sort((a, b) => {
      const dateA = new Date(a.date);
      const dateB = new Date(b.date);
      if (dateA.getTime() !== dateB.getTime()) {
        return dateA.getTime() - dateB.getTime();
      }
      return a.distance - b.distance;
    });

    // Sort farmers by distance
    allResults.farmers.sort((a, b) => a.distance - b.distance);

    // Combine all results into one final, sorted array.
    const combinedResults = [...allResults.farmers, ...allResults.markets];

    console.log("--- Final Combined Results (deduplicated and sorted) ---");
    console.log(combinedResults);
    res.status(200).json({ results: combinedResults });
  } catch (error) {
    console.error("--- Error Searching ---");
    console.error("Error:", error);
    res.status(500).json({ message: "Server error", error: error.message });
  }
});

module.exports = router;
