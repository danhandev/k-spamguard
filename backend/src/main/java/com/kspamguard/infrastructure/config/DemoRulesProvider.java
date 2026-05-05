package com.kspamguard.infrastructure.config;

import com.kspamguard.domain.rule.SpamRule;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoRulesProvider {

  @Bean
  List<SpamRule> defaultRules() {
    return List.of(
        // 무료쿠폰 (공백·특수문자 난독화 포함)
        SpamRule.regex("OBFUSCATED_FREE_COUPON", "무.{0,5}쿠폰", 0.45),
        // DM 유도
        SpamRule.regex("DM_LURE", "dm\\s*주세요|받.{0,5}dm", 0.45),
        // 팔로우·구독 유도
        SpamRule.keyword("FOLLOW_LURE", "팔로우", 0.3),
        SpamRule.keyword("SUBSCRIBE_LURE", "구독", 0.25),
        // URL 스팸
        SpamRule.regex("PHISHING_LINK", "https?://\\S+", 0.5),
        // 광고 키워드
        SpamRule.keyword("AD_EVENT", "이벤트 참여", 0.3),
        SpamRule.keyword("FREE_GIFT", "무료 선물", 0.4));
  }
}
