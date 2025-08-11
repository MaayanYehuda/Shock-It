const express = require("express");
const router = express.Router();
const neo4j = require("neo4j-driver");

// התחברות ל-NEO4J
const driver = neo4j.driver(
  "bolt://localhost:7687",
  // neo4j.auth.basic("neo4j", "loolrov17")
   neo4j.auth.basic("neo4j", "315833301")
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

// In your Node.js router file (e.g., `routes/items.js` or `routes/users.js`)

// 🆕 NEW: Route to update an item
router.put("/update", async (req, res) => {
  const {
    farmerEmail,
    originalItemName,
    newItemName,
    newPrice,
    newDescription,
  } = req.body;

  if (
    !farmerEmail ||
    !originalItemName ||
    !newItemName ||
    newPrice === undefined
  ) {
    return res
      .status(400)
      .json({ error: "Missing required fields for item update" });
  }
  if (typeof newPrice !== "number" || newPrice < 0) {
    return res.status(400).json({ error: "Invalid price" });
  }

  try {
    const result = await session.run(
      `
      MATCH (f:Person {email: $farmerEmail})-[:OFFERS]->(i:Item {name: $originalItemName})
      SET i.name = $newItemName, i.description = $newDescription, i.price = $newPrice
      RETURN i
      `,
      { farmerEmail, originalItemName, newItemName, newDescription, newPrice }
    );

    if (result.records.length === 0) {
      return res
        .status(404)
        .json({ error: "Item not found or not sold by this farmer" });
    }

    const updatedItem = result.records[0].get("i").properties;
    res.json({ message: "Item updated successfully", item: updatedItem });
  } catch (error) {
    console.error("Error updating item:", error);
    res.status(500).send("Server error updating item");
  }
});

// 🆕 NEW: Route to delete an item
// In your backend route file (e.g., itemsRoutes.js)

router.delete("/", async (req, res) => {
  const { farmerEmail, itemName } = req.body; // Assuming you've switched to query params as advised previously

  if (!farmerEmail || !itemName) {
    console.warn("Missing farmer email or item name for delete:", {
      farmerEmail,
      itemName,
    });
    return res.status(400).json({ error: "Missing farmer email or item name" });
  }

  console.log(
    `Attempting to delete item '${itemName}' for farmer '${farmerEmail}'`
  );

  try {
    const writeResult = await session.run(
      `
      MATCH (f:Person {email: $farmerEmail})-[r:OFFERS]->(i:Item {name: $itemName})
      DETACH DELETE r, i
      `,
      { farmerEmail, itemName }
    );

    if (writeResult.summary.counters.nodesDeleted > 0) {
      console.log(`Successfully deleted item '${itemName}' (Node deleted).`);
      res.json({ message: "Item deleted successfully" });
    } else if (writeResult.summary.counters.relationshipsDeleted > 0) {
      console.log(
        `Successfully deleted relationship for item '${itemName}' (Relationship deleted, item might have other relationships or be non-existent).`
      );
      res.json({
        message:
          "Item offer relationship deleted, item might persist if connected elsewhere.",
      });
    } else {
      console.warn(
        `No item '${itemName}' found for farmer '${farmerEmail}' to delete.`
      );
      res.status(404).json({
        message: "Item not found or already deleted for this farmer.",
      });
    }
  } catch (error) {
    console.error("Error deleting item:", error);
    // --- IMPORTANT CHANGE HERE ---
    res
      .status(500)
      .json({ error: "Server error deleting item: " + error.message }); // Send JSON error
  }
});

module.exports = router;
