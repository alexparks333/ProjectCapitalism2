package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ServerRules;

import java.util.List;

// The server's rules (and the Blood Moon Insurance price) as sent at login (network/RulesPacket); the defaults until then.
public final class ClientRules {

    public static volatile List<String> rules = ServerRules.DEFAULTS;
    // Blood Moon Insurance's one-time price in gold (server config), -1 until the server sends it
    public static volatile int bloodMoonInsurancePrice = -1;

    private ClientRules() {
    }
}