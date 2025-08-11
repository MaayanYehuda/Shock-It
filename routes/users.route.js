const express = require("express");
const router = express.Router();
const neo4j = require("neo4j-driver");
const bcrypt = require("bcrypt");
// התחברות ל-NEO4J
const driver = neo4j.driver(
  "bolt://localhost:7687", // כתובת בסיס הנתונים המקומי
  // neo4j.auth.basic("neo4j", "loolrov17")
   neo4j.auth.basic("neo4j", "315833301")
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
      "MATCH (u:Person {email: $email}) RETURN u",
      { email }
    );

    if (result.records.length === 0) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const user = result.records[0].get("u").properties;

    // השוואה בין הסיסמה שנשלחה ל-Hash שב-DB
    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

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
    // בדיקה אם המשתמש כבר קיים
    const checkResult = await session.run(
      "MATCH (u:Person {email: $email}) RETURN u",
      { email }
    );
    if (checkResult.records.length > 0) {
      return res.status(409).json({ message: "Email already exists" });
    }

    // יצירת hash לסיסמה
    const hashedPassword = await bcrypt.hash(password, 10); // 10 = salt rounds

    // שמירה ב-DB עם הסיסמה המוצפנת
    const result = await session.run(
      "CREATE (u:Person {email: $email, name: $name, phone: $phone, password: $password, address: $address}) RETURN u",
      { email, name, phone, password: hashedPassword, address }
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

router.put("/update", async (req, res) => {
  const { email, name, phone, address } = req.body; // Expects email, name, phone, address
  if (!email || !name || !phone || !address) {
    // Checks if all are present
    return res.status(400).json({ error: "Missing fields in request" });
  }
  try {
    const result = await session.run(
      `
      MATCH (u:Person {email: $email})
      SET u.name = $name, u.phone = $phone, u.address = $address
      RETURN u
      `,
      { email, name, phone, address }
    );

    if (result.records.length === 0) {
      return res.status(404).json({ error: "User not found" });
    }

    const user = result.records[0].get("u").properties;
    res.json({ message: "User updated successfully", user });
  } catch (error) {
    console.error("Error updating user:", error);
    res.status(500).send("Server error updating user");
  }
});

module.exports = router;
