INSERT INTO spam_rules (rule_code, rule_type, pattern, threshold, enabled, created_at, updated_at)
VALUES ('OBFUSCATED_FREE_COUPON', 'REGEX', '무.{0,5}쿠폰', 0.45, 1, NOW(6), NOW(6)),
       ('DM_LURE', 'REGEX', 'dm\\s*주세요|받.{0,5}dm', 0.45, 1, NOW(6), NOW(6)),
       ('FOLLOW_LURE', 'KEYWORD', '팔로우', 0.30, 1, NOW(6), NOW(6)),
       ('SUBSCRIBE_LURE', 'KEYWORD', '구독', 0.25, 1, NOW(6), NOW(6)),
       ('PHISHING_LINK', 'REGEX', 'https?://\\S+', 0.50, 1, NOW(6), NOW(6)),
       ('AD_EVENT', 'KEYWORD', '이벤트 참여', 0.30, 1, NOW(6), NOW(6)),
       ('FREE_GIFT', 'KEYWORD', '무료 선물', 0.40, 1, NOW(6), NOW(6));
