-- Supabase Database Migration for UNO Shield MDM
-- Run this in Supabase Dashboard → SQL Editor
-- This creates the initial schema for device management

-- ============================================
-- DEVICES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT UNIQUE NOT NULL,
    enrollment_id TEXT NOT NULL,
    serial_number TEXT,
    model TEXT,
    android_version TEXT,
    enrolled_at TIMESTAMPTZ DEFAULT NOW(),
    last_seen TIMESTAMPTZ,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'suspended', 'wiped')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- DEVICE POLICIES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS device_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    policy_type TEXT NOT NULL CHECK (policy_type IN (
        'app_blocking',
        'call_filtering',
        'restriction_policy',
        'network_policy',
        'security_policy',
        'display_policy'
    )),
    policy_data JSONB NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- BLACKLIST NUMBERS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS blacklist_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    phone_number TEXT NOT NULL,
    name TEXT,
    added_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, phone_number)
);

-- ============================================
-- WHITELIST NUMBERS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS whitelist_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    phone_number TEXT NOT NULL,
    name TEXT,
    added_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, phone_number)
);

-- ============================================
-- BLOCKED APPS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS blocked_apps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    name TEXT,
    blocked_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(device_id, package_name)
);

-- ============================================
-- DEVICE HEARTBEATS TABLE (for tracking)
-- ============================================
CREATE TABLE IF NOT EXISTS device_heartbeats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    battery_level INTEGER,
    network_type TEXT,
    location JSONB,
    metadata JSONB
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices(device_id);
CREATE INDEX IF NOT EXISTS idx_devices_enrollment_id ON devices(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_devices_status ON devices(status);
CREATE INDEX IF NOT EXISTS idx_policies_device_id ON device_policies(device_id);
CREATE INDEX IF NOT EXISTS idx_policies_type ON device_policies(policy_type);
CREATE INDEX IF NOT EXISTS idx_policies_active ON device_policies(is_active);
CREATE INDEX IF NOT EXISTS idx_blacklist_device_id ON blacklist_numbers(device_id);
CREATE INDEX IF NOT EXISTS idx_whitelist_device_id ON whitelist_numbers(device_id);
CREATE INDEX IF NOT EXISTS idx_blocked_apps_device_id ON blocked_apps(device_id);
CREATE INDEX IF NOT EXISTS idx_heartbeats_device_id ON device_heartbeats(device_id);
CREATE INDEX IF NOT EXISTS idx_heartbeats_timestamp ON device_heartbeats(timestamp DESC);

-- ============================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE blacklist_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE whitelist_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE blocked_apps ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_heartbeats ENABLE ROW LEVEL SECURITY;

-- ============================================
-- RLS POLICIES
-- ============================================

-- Devices can view and update their own data
CREATE POLICY "Devices can view own data" ON devices
    FOR SELECT USING (
        auth.uid()::text = device_id OR
        auth.jwt() ->> 'role' = 'service_role'
    );

CREATE POLICY "Devices can update own data" ON devices
    FOR UPDATE USING (
        auth.uid()::text = device_id OR
        auth.jwt() ->> 'role' = 'service_role'
    );

CREATE POLICY "Devices can insert own data" ON devices
    FOR INSERT WITH CHECK (
        auth.uid()::text = device_id OR
        auth.jwt() ->> 'role' = 'service_role'
    );

-- Device policies: devices can view their own policies
CREATE POLICY "Devices can view own policies" ON device_policies
    FOR SELECT USING (
        device_id IN (
            SELECT id FROM devices WHERE device_id = auth.uid()::text
        ) OR
        auth.jwt() ->> 'role' = 'service_role'
    );

-- Blacklist numbers: devices can manage their own
CREATE POLICY "Devices can manage own blacklist" ON blacklist_numbers
    FOR ALL USING (
        device_id IN (
            SELECT id FROM devices WHERE device_id = auth.uid()::text
        ) OR
        auth.jwt() ->> 'role' = 'service_role'
    );

-- Whitelist numbers: devices can manage their own
CREATE POLICY "Devices can manage own whitelist" ON whitelist_numbers
    FOR ALL USING (
        device_id IN (
            SELECT id FROM devices WHERE device_id = auth.uid()::text
        ) OR
        auth.jwt() ->> 'role' = 'service_role'
    );

-- Blocked apps: devices can manage their own
CREATE POLICY "Devices can manage own blocked apps" ON blocked_apps
    FOR ALL USING (
        device_id IN (
            SELECT id FROM devices WHERE device_id = auth.uid()::text
        ) OR
        auth.jwt() ->> 'role' = 'service_role'
    );

-- Heartbeats: devices can insert their own
CREATE POLICY "Devices can insert own heartbeats" ON device_heartbeats
    FOR INSERT WITH CHECK (
        device_id IN (
            SELECT id FROM devices WHERE device_id = auth.uid()::text
        ) OR
        auth.jwt() ->> 'role' = 'service_role'
    );

-- ============================================
-- FUNCTIONS & TRIGGERS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers to auto-update updated_at
CREATE TRIGGER update_devices_updated_at BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_device_policies_updated_at BEFORE UPDATE ON device_policies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to update device last_seen on heartbeat
CREATE OR REPLACE FUNCTION update_device_last_seen()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE devices
    SET last_seen = NEW.timestamp
    WHERE id = NEW.device_id;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to update last_seen on heartbeat insert
CREATE TRIGGER update_last_seen_on_heartbeat AFTER INSERT ON device_heartbeats
    FOR EACH ROW EXECUTE FUNCTION update_device_last_seen();

-- ============================================
-- STORAGE BUCKETS (run separately in Storage section)
-- ============================================
-- Note: Create these buckets in Supabase Dashboard → Storage
-- 
-- Bucket: device-logs
--   Public: false
--   File size limit: 10MB
--   Allowed MIME types: text/plain, application/json
--
-- Bucket: policy-configs
--   Public: false
--   File size limit: 1MB
--   Allowed MIME types: application/json

-- ============================================
-- SAMPLE DATA (optional, for testing)
-- ============================================
-- Uncomment to insert test data
/*
INSERT INTO devices (device_id, enrollment_id, model, android_version, status)
VALUES 
    ('test-device-001', 'enrollment-001', 'Pixel 7', '13', 'active'),
    ('test-device-002', 'enrollment-002', 'Samsung Galaxy S23', '13', 'active');

INSERT INTO device_policies (device_id, policy_type, policy_data)
SELECT id, 'app_blocking', '{"blocked_packages": ["com.example.app"]}'::jsonb
FROM devices WHERE device_id = 'test-device-001';
*/


