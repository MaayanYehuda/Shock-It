const express = require("express");
const router = express.Router();
const neo4j = require("neo4j-driver");

// התחברות ל-NEO4J
const driver = neo4j.driver(
  "bolt://localhost:7687",
  neo4j.auth.basic("neo4j", "loolrov17")
  // neo4j.auth.basic("neo4j", "315833301")
);

const session = driver.session();

// הוספת מוצר חדש ויצירת קשר לחקלאי
router.post("/add", async (req, res) => {
  const { name, description, price, farmerEmail } = req.body;

  if (!name || !description || !price || !farmerEmail) {
    return res.status(400).json({ error: "Missing fields in request" });
  }

  try {
    const result = await session.run(
      `
      MATCH (f:Person {email: $farmerEmail})
      CREATE (i:Item {name: $name, description: $description, price: $price})
      CREATE (f)-[:OFFERS]->(i)
      RETURN i, f
      `,
      { name, description, price: parseFloat(price), farmerEmail }
    );

    if (result.records.length === 0) {
      return res.status(404).json({ error: "Farmer not found" });
    }

    const item = result.records[0].get("i").properties;
    res.status(201).json({ message: "Item added successfully", item });
  } catch (error) {
    console.error("Error adding item:", error);
    res.status(500).send("Server error adding item");
  }
});

// לקבלת כל המוצרים של חקלאי לפי האימייל שלו
router.get("/", async (req, res) => {
  const { farmerEmail } = req.query;

  if (!farmerEmail) {
    return res.status(400).json({ error: "Missing farmerEmail parameter" });
  }

  try {
    const result = await session.run(
      `
      MATCH (f:Person {email: $farmerEmail})-[:OFFERS]->(i:Item)
      RETURN i
      `,
      { farmerEmail }
    );

    const items = result.records.map((record) => record.get("i").properties);
    res.json(items);
  } catch (error) {
    console.error("Error fetching items:", error);
    res.status(500).send("Server error fetching items");
  }
});

module.exports = router;
