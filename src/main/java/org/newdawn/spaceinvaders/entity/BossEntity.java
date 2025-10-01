package org.newdawn.spaceinvaders.entity;
import org.newdawn.spaceinvaders.Game;
import java.awt.*;

public class BossEntity extends Entity {
    private final Game game;
    private final int maxHp = 5000;// //현재 체력
    private int hp = maxHp;

    //총알
    public void applyBulletDamage(int base) {
        applyDamage(base);
    }
    //레이저
    public void applyLaserDamage(int base) {
        applyDamage(base * 8);
    }
    private void applyDamage(int damage) {
        hp = Math.max(0, hp - damage); //hp가 음수 가는 거 방지
        if (hp == 0) {
            game.removeEntity(this);
            game.notifyBossKilled();
        } //hp가 0이면 보스 kill
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShotEntity) { //총알 맞았을 때
            ShotEntity s = (ShotEntity) other;
            applyBulletDamage(s.getBaseDamage());
            game.removeEntity(s);
        } else if (other instanceof LaserEntity) { //레이저를 맞았을 때
            LaserEntity l = (LaserEntity) other;
            applyLaserDamage(l.getLaserDamage()); //레이저 데미지 적용
            game.removeEntity(l);
        }
    }

    public BossEntity(Game game, int x, int y) {
        super("sprites/boss.gif", x, y); //보스의 이미지와 좌표를 초기화
        this.game = game;
    }




    public void draw(Graphics g) {
        super.draw(g);
        //체력바 크기와 위치
        final int barW = 100;
        final int barH = 6;
        final int barY  = getY() - 10; //체력 바가 보스 머리 위에서 10만큼 떨어짐

        double ratio = Math.max(0.0, Math.min(1.0, hp / (double) maxHp));
        int curW = (int)Math.round(barW * ratio); //현재 체력바 너비
        int barX = getX() + getWidth()/2 - barW/2;

        if (ratio < 1.0) {
            g.setColor(java.awt.Color.red); //피가 깎인 만큼 체력바가 red로 변함
            g.fillRect(barX, barY, barW, barH); //사각형을 만듬
        }
        g.setColor(java.awt.Color.green); //남은 피는 green
        g.fillRect(barX, barY, curW, barH);
    }
}
