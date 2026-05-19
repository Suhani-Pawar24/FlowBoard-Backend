const jwt = require("jsonwebtoken");
const token = jwt.sign(
  { sub: "rahul@gmail.com", role: "USER", userId: 16 },
  "MyVerySecureSecretKeyForJWTSigningThatIsAtLeast32CharactersLongForHS256Algorithm",
  { expiresIn: "24h" }
);
console.log(token);
