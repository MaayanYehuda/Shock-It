const express = require("express");
const router = express.Router();
const neo4j = require("neo4j-driver");
const bcrypt = require("bcrypt");
const driver = neo4j.driver(
  "bolt://localhost:7687",
  neo4j.auth.basic("neo4j", "loolrov17")
  //  neo4j.auth.basic("neo4j", "315833301")
);

const session = driver.session();
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

//  התחברות של משתמש
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

// הוספת משתמש חדש
router.post("/register", async (req, res) => {
  const {
    email,
    name,
    phone,
    password,
    address,
    latitude,
    longitude,
    notificationRadius,
  } = req.body;

  let session;
  try {
    session = driver.session();

    // בדיקה אם המשתמש כבר קיים
    const checkResult = await session.run(
      "MATCH (u:Person {email: $email}) RETURN u",
      { email }
    );
    if (checkResult.records.length > 0) {
      return res.status(409).json({ message: "Email already exists" });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    let cypherQuery = `
            CREATE (u:Person {
                email: $email, 
                name: $name, 
                phone: $phone, 
                password: $hashedPassword, 
                address: $address
        `;

    const params = {
      email,
      name,
      phone,
      hashedPassword,
      address,
    };

    if (latitude) {
      cypherQuery += ", latitude: toFloat($latitude)";
      params.latitude = latitude;
    }
    if (longitude) {
      cypherQuery += ", longitude: toFloat($longitude)";
      params.longitude = longitude;
    }
    if (notificationRadius) {
      cypherQuery += ", notificationRadius: toInteger($notificationRadius)";
      params.notificationRadius = notificationRadius;
    }

    cypherQuery += "}) RETURN u";

    const result = await session.run(cypherQuery, params);

    const user = result.records[0].get("u").properties;
    res.status(201).json(user);
  } catch (error) {
    console.error(error);
    res.status(500).send("Error creating user");
  } finally {
    if (session) {
      session.close();
    }
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
  const {
    email,
    name,
    phone,
    address,
    longitude,
    latitude,
    notificationRadius,
  } = req.body;

  if (
    !email ||
    !name ||
    !phone ||
    !address ||
    notificationRadius === undefined ||
    !longitude ||
    !latitude
  ) {
    return res.status(400).json({ error: "Missing fields in request" });
  }

  try {
    const result = await session.run(
      `
      MATCH (u:Person {email: $email})
      SET u.name = $name, u.phone = $phone, u.address = $address,u.latitude=$latitude, u.longitude= $longitude ,u.notificationRadius = $notificationRadius
      RETURN u
      `,
      { email, name, phone, address, notificationRadius, longitude, latitude }
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

router.get("/findRecomendations", async (req, res) => {
  const { email } = req.query;

  if (!email) {
    return res.status(400).json({ error: "Missing user email" });
  }

  try {
    const cypherQuery = `
      // מצא את המשתמש ופרטי המיקום שלו
      MATCH (u:Person {email: $email})
      // מצא את כל השווקים
      MATCH (m:Market)
      // ודא שהשוק בטווח הרדיוס של המשתמש (נקודות המרחק מחושבות במטרים)
      WHERE point.distance(
        point({latitude: u.latitude, longitude: u.longitude}), 
        point({latitude: m.latitude, longitude: m.longitude})
      ) <= u.notificationRadius * 1000
      // סנן החוצה שווקים שהמשתמש כבר מחובר אליהם
      AND NOT (u)-[:INVITE|REQUEST|FOUNDER]->(m)
      RETURN m
    `;

    const result = await session.run(cypherQuery, { email });

    const markets = result.records.map((record) => record.get("m").properties);
    console.log("Found markets:", markets);
    res.json(markets);
  } catch (error) {
    console.error("Error finding recommendations:", error);
    res.status(500).send("Server error finding recommendations");
  }
});

router.get("/invitations/count", async (req, res) => {
  const { email } = req.query;

  if (!email) {
    return res.status(400).json({ error: "Missing user email" });
  }

  let session;
  try {
    session = driver.session();

    const cypherQuery = `
            MATCH (m:Market)-[i:INVITE]->(u:Person {email: $email})
            WHERE i.participate = false
            RETURN count(i) AS pendingInvitationsCount
        `;

    const result = await session.run(cypherQuery, { email });

    const pendingInvitationsCount = result.records[0].get(
      "pendingInvitationsCount"
    ).low;
    console.log("Pending invitations count:", pendingInvitationsCount);
    res.json({ count: pendingInvitationsCount });
  } catch (error) {
    console.error("Error fetching invitation count:", error);
    res.status(500).send("Server error fetching invitation count");
  } finally {
    if (session) {
      session.close();
    }
  }
});
module.exports = router;
