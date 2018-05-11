package hust.cc.asynchronousacousticlocalization.utils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import hust.cc.asynchronousacousticlocalization.activity.MainActivity;

public class TimeStamp {
    public int anchorId = 0;
    public int tdoa = 0;

    public TimeStamp(int anchorID, int tdoa){
        this.anchorId = anchorID;
        this.tdoa = tdoa;
    }

    public TimeStamp(){}

    public JSONObject formatMessage(){

        Map<String, Integer> map = new HashMap<>();
        map.put(FlagVar.identityStr, this.anchorId);
        map.put(FlagVar.tdoaStr, this.tdoa);

        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }
}
