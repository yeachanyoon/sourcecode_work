package org.newdawn.spaceinvaders.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;

import org.newdawn.spaceinvaders.Game;

/**
 * 레이저 아이템 & 레이저 빔(시각효과) 엔티티
 * - ITEM: 이미지 없이 도형으로 렌더. 천천히 아래로 떨어지고 Ship과 닿으면 습득.
 * - BEAM: 지정 시간(ms) 유지, 매 프레임 Ship의 중심 X를 따라가며 Game.tickLaserAt(cx, halfW)로 판정.
 */
public class LaserEntity extends Entity {

    public enum Mode { ITEM, BEAM }

    private final Game game;
    private final Mode mode;

    // ITEM 모드(도형)
    private static final int ITEM_W = 14;
    private static final int ITEM_H = 18;
    private static final int ITEM_FALL_SPEED = 120; // px/sec
    private boolean collected = false;

    // BEAM 모드
    private long expireAtMs;
    private int  beamHalfWidth = 1; // 시각적/판정 폭 절반
    private int  beamAlpha = 170;

    private LaserEntity(Game game, Mode mode, int x, int y) {
        // 부모는 스프라이트 경로가 필요하지만, BEAM에서는 사용하지 않음
        super("sprites/shot.gif", x, y);
        this.game = game;
        this.mode = mode;
        if (mode == Mode.BEAM) {
            this.sprite = null; // 부모 draw가 스프라이트 그리지 않도록
        }
    }

    /** 드랍되는 레이저 아이템 생성 (이미지 없이 도형) */
    public static LaserEntity createDropItem(Game game, int x, int y) {
        return new LaserEntity(game, Mode.ITEM, x, y);
    }

    /** ✅ Game에서 호출하는 팩토리: 활성 레이저 빔 생성 (durationMs 유지) */
    public static LaserEntity createActiveBeam(Game game, int centerX, int durationMs) {
        LaserEntity e = new LaserEntity(game, Mode.BEAM, centerX, 0);
        long now = System.currentTimeMillis();
        e.expireAtMs = now + (durationMs <= 0 ? 500 : durationMs); // 기본 0.5초
        return e;
    }

    /** 외부에서 BEAM 여부 체크할 때 사용(Game.isBeam 호출 대응) */
    public boolean isBeam() { return mode == Mode.BEAM; }

    @Override
    public void move(long delta) {
        if (mode == Mode.ITEM) {
            long dyLong = ((long) ITEM_FALL_SPEED * delta) / 1000L; // 정수 산술
            this.y += (int) dyLong;
            if (this.y > Game.VIRTUAL_HEIGHT + 50) {
                game.removeEntity(this);
            }
        } else {
            int cx = game.getShipCenterX();
            this.x = cx - 1;
            this.y = 0;

            // 지속 판정은 Game이 수행(사이드스텝 문제 방지)
            game.tickLaserAt(cx, beamHalfWidth);

            long now = System.currentTimeMillis();
            if (now >= expireAtMs) {
                game.removeEntity(this);
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        if (mode == Mode.ITEM) {
            // 붉은 다이아몬드 도형(아이템)
            int drawX = (int) x;
            int drawY = (int) y;
            int cx = drawX + ITEM_W / 2;
            int cy = drawY + ITEM_H / 2;
            int hw = ITEM_W / 2;
            int hh = ITEM_H / 2;

            Polygon diamond = new Polygon(
                    new int[] { cx, cx + hw, cx, cx - hw },
                    new int[] { cy - hh, cy, cy + hh, cy },
                    4
            );
            g.setColor(new Color(255, 80, 80, 230));
            g.fillPolygon(diamond);
            g.setColor(new Color(255, 0, 0, 240));
            g.drawPolygon(diamond);
        } else {
            // 레이저 빔: 화면을 가로지르는 직사각형(기체 앞에서 보이도록 Game에서 순서 조절 권장)
            int cx = game.getShipCenterX();
            int left   = cx - beamHalfWidth;
            int width  = beamHalfWidth * 2;
            int height = (int) Game.VIRTUAL_HEIGHT;

            g.setColor(new Color(255, 0, 0, beamAlpha));
            g.fillRect(left, 0, width, height);
            g.setColor(new Color(255, 200, 200, beamAlpha));
            g.drawRect(left, 0, width, height);
        }
    }

    /** 🚫 BEAM은 충돌 시스템에서 제외 (판정은 Game.tickLaserAt로 처리) + NPE 방어 */
    @Override
    public boolean collidesWith(Entity other) {
        if (mode == Mode.BEAM) return false;
        if (other == null) return false;
        if (other instanceof LaserEntity) {
            LaserEntity le = (LaserEntity) other;
            if (le.mode == Mode.BEAM) return false;
        }
        if (this.sprite == null || other.sprite == null) return false;
        return super.collidesWith(other);
    }

    @Override
    public void collidedWith(Entity other) {
        if (mode == Mode.ITEM) {
            if (collected) return;
            if (other instanceof ShipEntity) {
                if (game.collectLaser()) {
                    collected = true;
                    game.removeEntity(this);
                }
            }
        }
        // BEAM은 충돌 처리 없음(별도 판정)
    }

    @Override
    public void doLogic() {
        // 레이저는 별도의 논리 업데이트 없음
    }

    @Override
    public int getWidth() { return (mode == Mode.ITEM) ? ITEM_W : beamHalfWidth * 2; }

    @Override
    public int getHeight() { return (mode == Mode.ITEM) ? ITEM_H : (int) Game.VIRTUAL_HEIGHT; }
}
