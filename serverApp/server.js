const express = require("express");
const bodyParser = require("body-parser");
const sqlite3 = require("sqlite3").verbose();

//internal vars
var highestSpeed = -1;
var chargeGained = -1;
var previousCharge = 0;
var batteryCapacity = 3110400; //in watt seconds
var latestCharge = batteryCapacity;
var latestChargePercent = 0;
var latestSpeed = 0;
var latestSpeedArray = [];
var allSpeed = [];
var latestChargeArray = [];
const latestChargeArraySize = 1200; //currently: last ten minutes 
const latestSpeedArraySize = 600; //last five min
var allCharge = [];
var latestCurrent = 0;

var testingCounter = 100;

var app = express();

var server = app.listen(process.env.PORT || 3000, listen);

function listen() {
  var host = server.address().address;
  var port = server.address().port;
  console.log('Example app listening at http://' + host + ':' + port);
}

app.use(bodyParser.json({ limit: "3000mb" }));
app.use(bodyParser.urlencoded({ extended : false }));

//connect to database
const database = new sqlite3.Database('./dataStore.db', (err) => {
  if (err) {
    console.error(err.message);
  }
  console.log('Connected to the database.');
});

database.serialize(() => {
  database.run("CREATE TABLE IF NOT EXISTS cars(carId INT PRIMARY KEY, carName TEXT);");

  database.run("INSERT INTO cars VALUES (1, 'Gear Up Car');", (err) =>{
    if (err) {/*Do nothing because if this error it's because this record 
    already existed and it shouldn't be entered multiple times*/}
  });

  database.run("CREATE TABLE IF NOT EXISTS speedData(carId INT, value INT, timeEnt INT);");

  database.run("CREATE TABLE IF NOT EXISTS chargeData(carId INT, value INT, timeEnt INT);");

  database.run("CREATE TABLE IF NOT EXISTS currentData(carId INT, value INT, timeEnt INT);");

  database.run("CREATE TABLE IF NOT EXISTS voltageData(carId INT, value INT, timeEnt INT);");
  /*uncomment this to test that connection and data is correct
  database.all("SELECT * FROM cars", (err, rows) => {
    if (err) {
      throw err;
    }
    rows.forEach((row) => {
      console.log(row);
    });
  });*/
});

app.use(express.static('public'));
//server functions
app.get("/test", function(req, res){
    res.send("This is a test")
})

app.post("/update", (req, res) => {
  const data = req.body;
  console.log(`${data.indicator}: ${data.val}`);
  //insert values (carId, val, timestamp)
  // indicators will be: spe, cha, cur, vol\
  if (data.indicator == "spe"){ // speed is assumed to come in as miles per hour
    latestSpeed = Math.trunc(data.val);
    allSpeed.push(latestSpeed);
    if(latestSpeed > highestSpeed){
      highestSpeed = latestSpeed;
    }
    if(latestSpeedArray.length > latestSpeedArraySize){
      latestSpeedArray = latestSpeedArray.slice(1);
    }
    latestSpeedArray.push(latestSpeed);
    database.run(`INSERT INTO speedData VALUES (${data.carId},${data.val},${data.timeStamp}) `); 
  } 
  else if(data.indicator == "cha"){ //Charge is assumed to come in as Watt seconds
    previousCharge = latestCharge;
    if(data.val > batteryCapacity){
      latestCharge = batteryCapacity;
    } else if(data.val < 0){
      latestCharge = 0;
    } else {
      latestCharge = data.val;
    }
    //the speed is because when the car is idle we will sometimes get negative current values, 
    //so the idea is that the car will be moving when it goes over the pad
    if(latestSpeed > 0){ 
    //if(latestCurrent < -0.33){
      //if(previousCharge < latestCharge){
      //if((latestCharge - previousCharge) > 10){
        chargeGained += latestCharge - previousCharge;
        database.run(`INSERT INTO chargeGained VALUES (${data.carId},${chargeGained},${new Date().getTime() / 1000}) `);
      //}
    }
    //This takes off the oldest charge and adds the latest one
    if(latestChargeArray.length > latestChargeArraySize){
      latestChargeArray = latestChargeArray.slice(1);
    }
    latestChargeArray.push(latestCharge);
    latestChargePercent = Math.trunc((latestCharge / batteryCapacity) * 100);
    allCharge.push(latestCharge);
    database.run(`INSERT INTO chargeData VALUES (${data.carId},${data.val},${data.timeStamp}) `); 
  }
  else if(data.indicator == "cur"){
    latestCurrent = data.val;
    database.run(`INSERT INTO currentData VALUES (${data.carId},${data.val},${data.timeStamp}) `); 
  }
  else if (data.indicator == "vol"){
    database.run(`INSERT INTO voltageData VALUES (${data.carId},${data.val},${data.timeStamp}) `); 
  }
  //leave this
  res.end();
});

app.post("/getCarData", (req, res) =>{
  const carId = req.body.carId;
  database.all(`SELECT * FROM cars WHERE carId = ${carId}`, (err, rows) => {
    res.send(rows[0].carName);
  })
});

app.post("/getData", (req, res) => {
  const carId = req.body.carId;
  //this will always send a carId
  //console.log("A request for data.")
  /****************!!!!!!!!!!!!!!!!!ONLY for testing remove!!!!
  if(testingCounter < 0){
    latestCharge += 1;
    chargeGained += 1;
  } else {
    latestCharge -= 1;
  }
  if(testingCounter < -50){
    testingCounter = 100;
  }
  testingCounter -= 1;
  latestChargeArray = latestChargeArray.slice(1);
  latestChargeArray.push(latestCharge);
  latestChargePercent = Math.floor((latestCharge / batteryCapacity)*100);
  allCharge.push(latestCharge);
  //console.log(`current charge: ${latestCharge}   / ${batteryCapacity}`)
  if(latestCharge <= 0){
    //console.log("update latest charge")
    latestCharge = batteryCapacity;
    latestChargeArray = latestChargeArray.map(m => batteryCapacity);
  }
  /****************!!!!!!!!!!!!!!!!!ONLY for testing remove!!!!*/
  if(highestSpeed == -1){
    database.all(`SELECT MAX(value) FROM speedData WHERE carId = ${carId}`, (err, rows) => {
      if(rows.length != 0) {
        highestSpeed = rows[0]["MAX(value)"];
      }
    });
  }
  if(chargeGained == -1){
    database.all(`SELECT * FROM chargeGained WHERE carId = ${carId} order by timeEnt desc limit 1`, (err, rows) => {
      if(rows.length != 0) {
        chargeGained = rows[0].value;
      }
    });
  }
  res.send(`{ 
    "speed" : ${latestSpeed}, 
    "charge" : ${latestChargePercent},
    "highestSpeed" : ${highestSpeed},
    "chargeGained" : ${chargeGained} }`
  );
  /*
  res.write("{ ")    
  //TODO: query database for the data
  database.all(`SELECT * FROM speedData WHERE carId = ${carId} order by timeEnt desc limit 1`, (err, rows) => {
    if(rows.length != 0) {
      res.write(`"speed" : ${rows[0].value}, `);
    }
  });
  database.all(`SELECT * FROM chargeData WHERE carId = ${carId} order by timeEnt desc limit 1`, (err, rows) => {
    if(rows.length != 0) {
      res.write(`"charge" : ${rows[0].value}, `)
    }
  });
  setTimeout(() => {
    res.write(` "nothing" : 0 } `)
    res.end()
  }, 500);*/
});

app.post("/getDataForChart", (req, res) => {
  //console.log(req.body)
  const carId = req.body.carId;
  const chartType = req.body.chartType;
  if(chartType == "allCharge"){
    if(allCharge.length == 0){
      database.all(`SELECT value FROM chargeData WHERE carId = ${carId} order by timeEnt asc`, (err, rows) => {
        if(rows.length != 0) {
          allCharge = rows.map(r => r.value);
          latestCharge = rows[rows.length-1].value;
          latestChargePercent = Math.trunc((latestCharge / batteryCapacity) * 100);
        }
      });
    }
    res.send(` { "labels" : ${JSON.stringify(allCharge)}, "chargeData" : ${JSON.stringify(allCharge)} } `);
  } else if(chartType == "allSpeed"){
    if(allSpeed.length == 0){
      database.all(`SELECT value FROM speedData WHERE carId = ${carId} and value > 0 order by timeEnt asc`, (err, rows) => {
        if(rows.length != 0) {
          allSpeed = rows.map(r => r.value);
          latestCharge = rows[rows.length-1].value;
          latestChargePercent = Math.trunc((latestCharge / batteryCapacity) * 100);
        }
      });
    }
    res.send(` { "labels" : ${JSON.stringify(allSpeed)}, "chargeData" : ${JSON.stringify(allSpeed)} } `);
  } else if(chartType == "latestSpeed"){
    if(latestSpeedArray.length == 0){
      database.all(`SELECT value FROM speedData WHERE carId = ${carId} order by timeEnt desc limit ${latestSpeedArraySize}`, (err, rows) => {
        if(rows.length != 0) {
          latestSpeedArray = rows.map(r => r.value);
          latestSpeedArray.reverse();
        }
      });
    }
    res.send(` { "chargeData" : ${JSON.stringify(latestSpeedArray)}, "percent" : ${latestChargePercent} } `);
  } else if(chartType == "latestCharge"){
    if(latestChargeArray.length == 0){
      database.all(`SELECT value FROM chargeData WHERE carId = ${carId} order by timeEnt desc limit ${latestChargeArraySize}`, (err, rows) => {
        if(rows.length != 0) {
          latestChargeArray = rows.map(r => r.value);
          latestChargeArray.reverse();
        }
      });
    }
    res.send(` { "chargeData" : ${JSON.stringify(latestChargeArray)}, "percent" : ${latestChargePercent} } `);
  }
  /*res.write("[")    
  database.all(`SELECT * FROM chargeData WHERE carId = ${carId} order by timeEnt desc limit 10`, (err, rows) => {
    if(rows.length != 0) {
      res.write(rows.map(row => row.value).join(","));
    }
  });
  setTimeout(() => {
    res.write("]")  
    res.end()
  }, 1000);*/
});

app.on('exit', function() {
  db.close((err) => {
    if (err) {
      return console.error(err.message);
    }
    console.log('Close the database connection.');
  });
});