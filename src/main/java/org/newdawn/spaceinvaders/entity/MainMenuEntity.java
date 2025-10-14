package org.newdawn.spaceinvaders.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.newdawn.spaceinvaders.Game;

public class MainMenuEntity extends Entity {
    private final Game game;
    private boolean visible = true;

    public MainMenuEntity(Game game) {
        // 실제 스프라이트는 쓰지 않지만 부모 생성자 시그니처를 맞추기 위해 전달
        super("sprites/ship.gif", 0, 0);
        this.game = game;
    }

    public void move(long delta) { /* 메뉴는 이동 없음 */ }
    public void doLogic() { }
    public void collidedWith(Entity other) { }

    // ✅ 부모와 동일 시그니처로 수정: Graphics
    public void draw(Graphics g0) {
        if (!visible) return;
        Graphics2D g = (Graphics2D) g0;

        // 반투명 오버레이
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, 800, 600);

        // 타이틀
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        String title = "SPACE INVADERS";
        FontMetrics fm = g.getFontMetrics();
        int x = (800 - fm.stringWidth(title)) / 2;
        g.drawString(title, x, 200);

        // 안내 문구
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String sub = "Play / 도전과제 버튼을 눌러주세요";
        x = (800 - g.getFontMetrics().stringWidth(sub)) / 2;
        g.drawString(sub, x, 240);
    }

    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible() { return visible; }
}
