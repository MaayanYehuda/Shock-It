const express = require("express");
const router = express.Router();
neo4j = require("neo4j-driver");

const driver = neo4j.driver(
  "bolt://localhost:7687", // כתובת בסיס הנתונים המקומי
  neo4j.auth.basic("neo4j", "loolrov17") // שים את הסיסמה שלך
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
  console.log("Request headers:", req.headers);

  const { date, latitude, location, longitude } = req.body;

  // בדיקת נתונים נדרשים
  if (!date || !location || latitude == null || longitude == null) {
    console.log("Missing required fields");
    return res.status(400).json({
      message: "Missing required fields",
      required: ["date", "location", "latitude", "longitude"],
      received: { date, location, latitude, longitude },
    });
  }

  try {
    console.log("Checking if market exists:", { date, location });

    // בדיקה אם השוק כבר קיים
    const checkResult = await session.run(
      "MATCH (m:Market {date: $date, location: $location}) RETURN m",
      { date, location }
    );

    if (checkResult.records.length > 0) {
      console.log("Market already exists");
      return res.status(409).json({ message: "Market already exists" });
    }

    console.log("Creating new market...");

    // יצירת השוק החדש
    const result = await session.run(
      "CREATE (m:Market {date: $date, latitude: $latitude, location: $location, longitude: $longitude}) RETURN m",
      {
        date,
        latitude: parseFloat(latitude),
        location,
        longitude: parseFloat(longitude),
      }
    );

    const market = result.records[0].get("m").properties;
    console.log("Market created successfully:", market);
    res.status(201).json({
      message: "Market created successfully",
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
module.exports = router;
