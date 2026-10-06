-- 种子数据：测试账号 / 社区 / 楼栋 / 机器人 / 载物仓

insert into user_account (username, password, display_name, role) values
    ('admin',    'admin123',    '系统管理员',      'ADMIN'),
    ('property', 'property123', '物业-阳光社区',   'PROPERTY'),
    ('ops',      'ops123',      '运营专员',        'OPS'),
    ('cs',       'cs123',       '客服专员',        'CS');

insert into community (name, night_restricted, night_start, night_end) values
    ('阳光社区', true,  '22:00', '07:00'),   -- id=1 物业要求限制夜间运行
    ('绿洲家园', false, '22:00', '07:00');   -- id=2

insert into building (community_id, name, floors) values
    (1, '1栋', 12),   -- id=1
    (1, '2栋', 18),   -- id=2
    (2, '5栋', 8);    -- id=3

insert into robot (code, community_id, battery, status) values
    ('RB-001', 1, 90, 'IDLE'),   -- id=1
    ('RB-002', 1, 15, 'IDLE'),   -- id=2 低电量，用于演示电量不足
    ('RB-003', 2, 80, 'IDLE');   -- id=3

insert into cargo_compartment (robot_id, compartment_no, cold_chain_capable, status) values
    (1, 'A', true,  'IDLE'),
    (1, 'B', false, 'IDLE'),
    (2, 'A', true,  'IDLE'),
    (3, 'A', true,  'IDLE'),
    (3, 'B', false, 'IDLE');
