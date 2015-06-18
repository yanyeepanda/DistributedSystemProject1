/**
 * @author Yanyi Liang
 * @date Sep 2014
 */

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CounterJSON {
	private static final JSONParser parser = new JSONParser();
	
	public CounterJSON(){
		
	}
	
	public String CounterInstJSON(String jst, int counter){
		JSONObject counterObj = new JSONObject();
		JSONObject obj;
		try {
			obj = (JSONObject) parser.parse(jst);
		} catch (ParseException e) {
			// alert the user
			e.printStackTrace();
			return null;
		}
		counterObj.put("inst", obj);
		counterObj.put("type", "inst");
		counterObj.put("counter", counter);
		
		return counterObj.toJSONString();	
	}
	
	public String CounterExpectJSON(int counter){
		JSONObject counterObj = new JSONObject();
		
		counterObj.put("type", "expecting");
		counterObj.put("counter", counter);
		
		return counterObj.toJSONString();	
	}
	
	public String CounterAckJSON(int counter){
		JSONObject counterObj = new JSONObject();
		
		counterObj.put("type", "ack");
		counterObj.put("counter", counter);
		
		return counterObj.toJSONString();	
	}
	
	public String CounterExceptionJSON(int counter){
		JSONObject counterObj = new JSONObject();
		
		counterObj.put("type", "exception");
		counterObj.put("counter", counter);
		
		return counterObj.toJSONString();	
	}
	
	public JSONObject getCounterJSON(String CJSONStr){
		JSONObject obj;
		try {
			obj = (JSONObject) parser.parse(CJSONStr);
		} catch (ParseException e) {
			// alert the user
			e.printStackTrace();
			return null;
		}
		return obj;
	}

	public String getCounterJSONType(JSONObject obj){
		if(obj!=null){
			String JSONType=null;
			if(obj.get("type").equals("expecting"))
				JSONType = "expecting";
			else if(obj.get("type").equals("ack"))
				JSONType = "ack";
			else if(obj.get("type").equals("exception"))
				JSONType = "exception";
			else if(obj.get("type").equals("inst"))
				JSONType = "inst";
			else if(obj.get("type").equals("negotiation"))
				JSONType = "negotiation";
			return JSONType;
		} else return null;
	}
	
	public int getJSONCounter(JSONObject obj){
		if(obj!=null){
			int Counter = ((Long) obj.get("counter")).intValue();
			return Counter ;
		}else return 0;
	}

	public String getJSONInst(JSONObject obj){
		if(obj.get("type").equals("inst")){
			JSONObject inst = (JSONObject) obj.get("inst");
			return inst.toJSONString() ;
		}else return null;
	}
}
