// server.js
const express = require("express");
const cors = require("cors");


const app = express();
const port = 3000;

// אפשר קריאות מכל מקום (לצורך הצגה)
app.use(cors());
app.use(express.json());


let users = require("./routes/users.route");
app.use("/users", users);

// הפעלת השרת
app.listen(port, "0.0.0.0", () => {
  console.log(`Server is running on http://localhost:${port}`);
});
