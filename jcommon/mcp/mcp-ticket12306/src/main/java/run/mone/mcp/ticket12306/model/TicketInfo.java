package run.mone.mcp.ticket12306.model;

import lombok.Data;

@Data
public class TicketInfo {

    private String trainCode; // 车次号
    private String startStation; // 始发站代码
    private String endStation; //终点站代码
    private String yourStart; //你坐车的始发
    private String yourEnd; //你坐车的目的地
    private String yourStartTime; // 出发时间
    private String yourEndTime; // 到达时间
    private String duration; // 运行时长
    private boolean canBook; // 是否可预订
    private String hardSeat; // 硬座余票
    private String softSleeper; // 软卧余票
    private String hardSleeper; // 硬卧余票

    public static TicketInfo parse(String data) {
        String[] fields = data.split("\\|");
        TicketInfo info = new TicketInfo();
        info.trainCode = fields[3];
        info.startStation = fields[4];
        info.endStation = fields[5];
        info.yourStart = fields[6];
        info.yourEnd = fields[7];
        info.yourStartTime = fields[8];
        info.yourEndTime = fields[9];
        info.duration = fields[10];
        info.canBook = "Y".equals(fields[11]);
        info.hardSeat = "无".equals(fields[30]) ? "0" : fields[30];
        info.softSleeper = "无".equals(fields[31]) ? "0" : fields[31];
        info.hardSleeper = "无".equals(fields[32]) ? "0" : fields[32];
        return info;
    }
}
