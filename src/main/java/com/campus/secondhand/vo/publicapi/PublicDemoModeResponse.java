package com.campus.secondhand.vo.publicapi;

/**
 * 公共端演示模式状态:只暴露前端展示所需的布尔开关,
 * 不暴露演示数据注入情况与业务统计(demoSummary 仅管理端可见)。
 */
public record PublicDemoModeResponse(
        boolean demoModeEnabled,
        boolean demoItemNotesEnabled
) {
}
