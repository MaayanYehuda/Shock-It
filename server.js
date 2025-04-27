// server.js
const express = require('express');
const cors = require('cors');
const neo4j = require('neo4j-driver');

const app = express();
const port = 3000;

// אפשר קריאות מכל מקום (לצורך הצגה)
app.use(cors());
app.use(express.json());

// התחברות ל-NEO4J
const driver = neo4j.driver(
  'bolt://localhost:7687',   // כתובת בסיס הנתונים המקומי
  neo4j.auth.basic('neo4j', '315833301')  // שים את הסיסמה שלך
);

const session = driver.session();

// דוגמה: קבלת כל הצמתים מסוג "User"
app.get('/', async (req, res) => {
  try {
    const result = await session.run('MATCH (u:Person) RETURN u');
    const users = result.records.map(record => record.get('u').properties);
    res.json(users);
  } catch (error) {
    console.error(error);
    res.status(500).send('Error fetching users');
  }
});

// הפעלת השרת
app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
