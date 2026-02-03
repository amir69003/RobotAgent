package Util;

import world.Position;
import java.util.concurrent.ConcurrentHashMap;

public class AgentRegistry {
    public static final ConcurrentHashMap<String, Position> positions = new ConcurrentHashMap<>();
}