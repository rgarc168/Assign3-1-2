import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 *
 */
public class SockServer {
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;

  static int port = 8888;

  public static void main (String args[]) {

    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }

    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      //open socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections");

      /**
       * Simple loop accepting one client and calling handling one request.
       *
       */


      while (true){
        System.out.println("Server waiting for a connection");
        sock = serv.accept(); // blocking wait
        System.out.println("Client connected");

        // setup the object reading channel
        in = new ObjectInputStream(sock.getInputStream());

        // get output channel
        OutputStream out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new DataOutputStream(out);

        boolean connected = true;
        while (connected) {
          String s = "";
          try {
            s = (String) in.readObject(); // attempt to read string in from client
          } catch (Exception e) { // catch rough disconnect
            System.out.println("Client disconnect");
            connected = false;
            continue;
          }

          JSONObject res = isValid(s);

          if (res.has("ok")) {
            writeOut(res);
            continue;
          }

          JSONObject req = new JSONObject(s);

          res = testField(req, "type");
          if (!res.getBoolean("ok")) { // no "type" header provided
            res = noType(req);
            writeOut(res);
            continue;
          }
          // check which request it is (could also be a switch statement)
          if (req.getString("type").equals("echo")) {
            res = echo(req);
          } else if (req.getString("type").equals("add")) {
            res = add(req);
          } else if (req.getString("type").equals("addmany")) {
            res = addmany(req);
          } else if (req.getString("type").equals("roller")) {
            res = roller(req);
          } else if (req.getString("type").equals("inventory")) {
            res = inventory(req);
          } else {
            res = wrongType(req);
          }
          writeOut(res);
        }
        // if we are here - client has disconnected so close connection to socket
        overandout();
      }
    } catch(Exception e) {
      e.printStackTrace();
      overandout(); // close connection to socket upon error
    }
  }


  /**
   * Checks if a specific field exists
   *
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");
    if (res.getBoolean("ok")) {
      if (!req.get("data").getClass().getName().equals("java.lang.String")){
        res.put("ok", false);
        res.put("message", "Field data needs to be of type: String");
        return res;
      }

      res.put("type", "echo");
      res.put("echo", "Here is your echo: " + req.getString("data"));
    }
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    res.put("ok", true);
    res.put("type", "add");
    try {
      res.put("result", req.getInt("num1") + req.getInt("num2"));
    } catch (org.json.JSONException e){
      res.put("ok", false);
      res.put("message", "Field num1/num2 needs to be of type: int");
    }
    return res;
  }

  // TODO: 2 "services" need to implement
  // implement me in assignment 3
  static JSONObject inventory(JSONObject req) {
    System.out.print("Inventory request: " + req.toString());
    JSONObject res2 = new JSONObject();
    
    Map<String, Integer> invent = new HashMap<>();
    String task = req.getString("task");
    //JSONArray inventoryTask = new JSONArray();
    switch (task) {
      case "add":
        //Implement this function
        res2 = addInventory(req, invent);
        break;
      case "view":
        //Implement this function
        res2 = viewInventory(invent);
        break;
      case "buy":
        //Implement this function
        res2 = buyInventory(req, invent);
        break;
      default:
        res2.put("ok", false);
        res2.put("message", "Invalid inventory task: " + task);
        break;
    }
    return res2;
  }
  // implementing the add, view and buy methods for the Inventory 
  /**
   * @param req
   * @param inventory
   * @return
   */
  static JSONObject addInventory(JSONObject reqAdd, Map<String, Integer> inventory) {
    JSONObject resAdd = new JSONObject();
    String productName = reqAdd.getString("productName");
    int quantity = reqAdd.getInt("quantity");

    if(quantity <= 0) {
      resAdd.put("ok", false);
      resAdd.put("message", "Quantity cannot be zero");
    }
    else {
      inventory.put(productName, inventory.getOrDefault(productName, 0) + quantity);
      resAdd.put("ok", true);
      resAdd.put("message", "Add: "+quantity+" "+productName+"(s) into the inventory.");
    }

    return resAdd;
  }
  // Veiw Inventory Method
  static JSONObject viewInventory(Map<String, Integer> inventory) {
    JSONObject resView = new JSONObject();
    JSONArray arr = new JSONArray();

    for(Map.Entry<String, Integer> entry : inventory.entrySet()) {
      JSONObject obj = new JSONObject();
      obj.put("item", entry.getKey());
      obj.put("quantity", entry.getValue());

      arr.put(obj);
    }

    resView.put("ok", true);
    resView.put("items", arr);

    return resView;
  }
  // Buy Inventory Method
  static JSONObject buyInventory(JSONObject reqBuy, Map<String, Integer> inventory) {
    JSONObject resBuy = new JSONObject();
    String productName = reqBuy.getString("item");
    int quantity = reqBuy.getInt("quantity");
    if (!inventory.containsKey(productName)) {
      resBuy.put("ok", false);
      resBuy.put("message", "Item " + productName + " not found in inventory.");
  } else {
      int availableQuantity = inventory.get(productName);
      if (availableQuantity >= quantity) {
          inventory.put(productName, availableQuantity - quantity);
          resBuy.put("ok", true);
          resBuy.put("message", "Purchased " + quantity + " " + productName + "(s) from inventory.");
      } else {
        resBuy.put("ok", false);
        resBuy.put("message", "Insufficient quantity of " + productName + " in inventory.");
      }
  }
  return resBuy;
  }


  // TODO: not calculating correctly
  // implement me in assignment 3
  static JSONObject roller(JSONObject req) {
    System.out.println("Roller request: " + req.toString());
    // Create response
    JSONObject res3 = testField(req, "faces");
    //int dieCount = req.getInt("dieCount");
    //int faces = req.getInt("faces");
    //check valid dieCount and faces
    /*if(dieCount <= 0 || faces <= 0) {
      res3.put("ok", false);
      res3.put("type", "roller");
      res3.put("message", "Die count and faces must be greater than 0."); 
    }*/
    if(!res3.getBoolean("ok")) {
      return res3;
    }
    
    int faces = req.getInt("faces");
    int dieCount = (int) (Math.random() * faces) + 1;

    JSONObject res4 = new JSONObject();

    res4.put("ok", true);
    res4.put("type", "roller");
    res4.put("result", dieCount); 

    return res4;
  }
  

  // handles the simple addmany request
  static JSONObject addmany(JSONObject req){
    System.out.println("Add many request: " + req.toString());
    JSONObject res = testField(req, "nums");
    if (!res.getBoolean("ok")) {
      return res;
    }

    int result = 0;
    JSONArray array = req.getJSONArray("nums");
    for (int i = 0; i < array.length(); i ++){
      try{
        result += array.getInt(i);
      } catch (org.json.JSONException e){
        res.put("ok", false);
        res.put("message", "Values in array need to be ints");
        return res;
      }
    }

    res.put("ok", true);
    res.put("type", "addmany");
    res.put("result", result);
    return res;
  }

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }

  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {e.printStackTrace();}

  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();

    } catch(Exception e) {e.printStackTrace();}

  }
}