const express = require("express");
const router = express.Router();
const neo4j = require("neo4j-driver");
// התחברות ל-NEO4J
const driver = neo4j.driver(
  "bolt://localhost:7687", // כתובת בסיס הנתונים המקומי
  neo4j.auth.basic("neo4j", "315833301") // שים את הסיסמה שלך
);

const session = driver.session();
// דוגמה: קבלת כל הצמתים מסוג "User"
router.get("/", async (req, res) => {
  try {
    const result = await session.run("MATCH (u:Person) RETURN u");
    const users = result.records.map((record) => record.get("u").properties);
    res.json(users);
  } catch (error) {
    console.error(error);
    res.status(500).send("Error fetching users");
  }
});

// דוגמה: התחברות של משתמש
router.get("/login", async (req, res) => {
  const { email, password } = req.query;

  if (!email || !password) {
    return res.status(400).json({ error: "Missing email or password" });
  }

  try {
    const result = await session.run(
      "MATCH (u:Person {email: $email, password: $password}) RETURN u",
      { email, password }
    );

    if (result.records.length === 0) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const user = result.records[0].get("u").properties;
    res.status(200).json(user);
  } catch (error) {
    console.error("Login error:", error);
    res.status(500).send("Server error during login");
  }
});

// דוגמה: הוספת משתמש חדש
router.post("/register", async (req, res) => {
  const { email, name, phone, password, address } = req.body;
  try {
    // בדיקה אם קיים משתמש עם אותו אימייל
    const checkResult = await session.run(
      "MATCH (u:Person {email: $email}) RETURN u",
      { email }
    );

    if (checkResult.records.length > 0) {
      return res.status(409).json({ message: "Email already exists" }); // Conflict
    }

    // יצירת משתמש חדש
    const result = await session.run(
      "CREATE (u:Person {email: $email, name: $name, phone: $phone, password: $password, address: $address}) RETURN u",
      { email, name, phone, password, address }
    );

    const user = result.records[0].get("u").properties;
    res.status(201).json(user);
  } catch (error) {
    console.error(error);
    res.status(500).send("Error creating user");
  }
});

// קבלת פרטי משתמש לפי אימייל
router.get("/profile", async (req, res) => {
  const { email } = req.query;

  if (!email) {
    return res.status(400).json({ error: "Missing email" });
  }

  try {
    const result = await session.run(
      "MATCH (u:Person {email: $email}) RETURN u",
      { email }
    );

    if (result.records.length === 0) {
      return res.status(404).json({ error: "User not found" });
    }

    const user = result.records[0].get("u").properties;
    res.json(user);
  } catch (error) {
    console.error("Profile fetch error:", error);
    res.status(500).send("Server error");
  }
});




module.exports = router;
