package org.newdawn.spaceinvaders.entity;

public class AlienState {
    public double x;
    public double y;
    public double dx; // 이동 방향 및 속도도 저장

    public AlienState() {}
    public AlienState(double x, double y, double dx) {
        this.x = x;
        this.y = y;
        this.dx = dx;
    }
}
