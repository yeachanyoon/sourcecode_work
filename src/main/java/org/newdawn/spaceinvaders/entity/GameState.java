package org.newdawn.spaceinvaders.entity;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public PlayerState playerState;
    public List<AlienState> alienStates;
    public int alienCount;
    public int score; // 스코어 필드 추가
    //기체강화용
    public double moveSpeed;
    public long firingInterval;
    public int bulletCount;
    public boolean speedUpgradeApplied;
    public boolean bulletCountUpgradeApplied;

    public GameState() {} // SnakeYAML이 역직렬화할 때 필요
}
