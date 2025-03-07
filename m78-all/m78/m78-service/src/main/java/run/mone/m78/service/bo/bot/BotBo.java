package run.mone.m78.service.bo.bot;

import com.xiaomi.mone.http.docs.annotations.HttpApiDocClassDefine;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author caobaoyu
 * @description:
 * @date 2024-03-01 15:44
 */
@Data
public class BotBo implements Serializable {

    @HttpApiDocClassDefine(value = "id", description = "机器人id")
    private Long id;

    @HttpApiDocClassDefine(value = "name", description = "机器人名称")
    private String name;

    @HttpApiDocClassDefine(value = "workspaceId", description = "工作空间id")
    private Long workspaceId;

    @HttpApiDocClassDefine(value = "avatarUrl", description = "机器人头像")
    private String avatarUrl;

    @HttpApiDocClassDefine(value = "creator", description = "创建人")
    private String creator;

    @HttpApiDocClassDefine(value = "remark", description = "备注")
    private String remark;

    @HttpApiDocClassDefine(value = "permissions", description = "权限")
    private Integer permissions;

    @HttpApiDocClassDefine(value = "publishStatus", description = "发布状态")
    private Integer publishStatus;

    private LocalDateTime publishTime;

    @HttpApiDocClassDefine(value = "publishStatusDesc", description = "发布状态描述")
    private String publishStatusDesc;

    @HttpApiDocClassDefine(value = "botUseTimes", description = "bot使用次数")
    private Long botUseTimes;

    @HttpApiDocClassDefine(value = "bot_avg_star", description = "机器人平均评分")
    private Double botAvgStar;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String updator;

    private Map<String, String> meta = new HashMap<>();

    private Boolean Collected;

}
