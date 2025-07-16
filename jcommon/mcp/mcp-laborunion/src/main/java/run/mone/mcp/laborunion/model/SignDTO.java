package run.mone.mcp.laborunion.model;

import lombok.Data;

@Data
public class SignDTO {
    private Long id;
    private String userName;
    private String remark;
    private String createDate;
    private String createBy;
    private String updateDate;
    private String updateBy;
    private Integer signType;
}
