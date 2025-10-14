package org.newdawn.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.AlienShotEntity;
import org.newdawn.spaceinvaders.entity.AsteroidEntity;
import org.newdawn.spaceinvaders.entity.BlackHoleEntity;
import org.newdawn.spaceinvaders.entity.BombEntity;
import org.newdawn.spaceinvaders.entity.BossEntity;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.LaserEntity;
import org.newdawn.spaceinvaders.entity.PlayScoreEntity;
import org.newdawn.spaceinvaders.entity.RankingScoreEntity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

public class Game extends Canvas {
	public static final int VIRTUAL_WIDTH  = 800;
	public static final int VIRTUAL_HEIGHT = 600;

	private BufferStrategy strategy;
	private boolean gameRunning = true;

	private ArrayList<Entity> entities   = new ArrayList<>();
	private ArrayList<Entity> removeList = new ArrayList<>();
	private Entity ship;

	private double moveSpeed = 300;
	private long lastFire = 0;
	private long firingInterval = 500;

	private String message = "SPACE INVADERS";
	private boolean waitingForKeyPress = true;
	private boolean leftPressed = false;
	private boolean rightPressed = false;
	private boolean firePressed = false;

	// 폭탄
	private boolean bombPressed = false;
	private long lastBombFire = 0;
	private long bombFireInterval = 400;
	private int bombCount = 0;
	private final int bombMax = 2;

	// 레이저
	private boolean laserPressed = false;
	private long lastLaserUse = 0;
	private long laserCooldown = 500;
	private int  laserCount = 0;
	private final int laserMax = 1;
	private final int LASER_STRIPE_HALF = 20; // 기본 폭(강화로 증가)

	private boolean logicRequiredThisLoop = false;
	private long lastFpsTime;
	private int fps;
	private String windowTitle = "Space Invaders 102";
	private JFrame container;

	// 메뉴/화면 상태
	private final MenuButton playButton       = new MenuButton("Play");
	private final MenuButton challengeButton  = new MenuButton("도전과제");
	private final MenuButton shopButton       = new MenuButton("상점");
	private final MenuButton reinforceButton  = new MenuButton("강화");
	private final MenuButton homeButton       = new MenuButton("메인화면");
	private final MenuButton backButton       = new MenuButton("← 돌아가기");

	// ✅ 설정(우상단) & 설정 패널 내 초기화 버튼
	private final MenuButton settingsButton     = new MenuButton("⚙ 설정");
	private final MenuButton resetInSettingsBtn = new MenuButton("초기화");
	private boolean showingSettings = false;

	private boolean showHomeOnMenu   = false;
	private boolean showingChallenge = false;
	private boolean showingLevelSelect = false;
	private boolean showingShipSelect  = false;
	private boolean showingReinforce   = false;
	private boolean showingShop        = false;

	private final MenuButton[] levelButtons = {
			new MenuButton("Level 1"),
			new MenuButton("Level 2"),
			new MenuButton("Level 3"),
			new MenuButton("Level 4"),
			new MenuButton("Level 5")
	};
	private final boolean[] levelUnlocked = new boolean[5];
	private int selectedLevel = -1;

	private final MenuButton[] selectButtons = {
			new MenuButton("선택하기"),
			new MenuButton("선택하기"),
			new MenuButton("선택하기")
	};
	private int selectedShipIndex = -1;

	// 미리보기/하트
	private Sprite ship1Sprite, ship2Sprite, ship3Sprite, ship3ProSprite;
	private Sprite heartFullSprite, heartEmptySprite;

	// 목숨
	private final int maxLives = 3;
	private int lives = maxLives;

	// 런 통계/도전과제
	private int totalKills = 0;
	private boolean achKill10   = false;
	private int shotsFiredRun   = 0;
	private boolean achClear100 = false;
	private long runStartTime   = 0;
	private long lastRunElapsedMs = -1;
	private boolean achClear1Min  = false;

	// 기체 #3 방어막
	private boolean shieldActive  = false;
	private long invulnUntil      = 0;
	private int  killsSinceLastShield = 0;

	// 토스트
	private String toastText = null;
	private long toastUntil  = 0;

	// 적 사격/스폰
	private long lastAlienFire = 0;
	private long alienFireInterval = 1200;

	private long nextAsteroidSpawn = 0;
	private long asteroidInterval = 3000;

	// 블랙홀(플레이어만 감속)
	private long lastBlackHoleSpawn = 0;
	private long blackHoleInterval = 15000;
	private float blackHoleRadius   = 180f;
	private long  blackHoleLifeMs   = 7000;

	// 점수 / 랭킹 UI
	private PlayScoreEntity playScoreUI;
	private RankingScoreEntity rankingUI;

	// 보스
	private BossEntity boss = null;
	private boolean bossSpawned = false;
	private boolean bossDefeated = false;

	// 랜덤
	private final Random rng = new Random();

	/* ========================= SAVE / RANK ========================= */
	private static class SaveState {
		int  highestUnlockedLevel = 1;
		boolean achKill10   = false;
		boolean achClear100 = false;
		boolean achClear1Min = false;

		int totalKillsOverall = 0;
		int bestTimeMs = -1;
		int lastSelectedShipIndex = -1;

		List<RankRow> leaderboard = new ArrayList<>();
		String playerName = System.getProperty("user.name", "PLAYER");

		// 강화
		int reinforcePoints = 0;
		int lvSpeed = 0, lvFireRate = 0, lvShield = 0, lvBomb = 0, lvLaser = 0;

		// 상점
		int coins = 0;
		int selectedSkinIndex = 0;
		String ownedSkinsCsv = "";
	}
	private SaveState saveData = new SaveState();

	// 점수 / 랭킹
	private int score = 0;
	public static class RankRow { public final String name; public final int score; public RankRow(String n,int s){name=n;score=s;} }
	private static final int LEADERBOARD_MAX = 10;

	// 배점
	private static final int SCORE_ALIEN = 100;
	private static final int SCORE_AST   = 50;
	private static final int SCORE_ALIEN_SHOT = 25;

	/* ========================= 강화 상태/상수 ========================= */
	private int reinforcePoints = 0;
	private static final int REINF_MAX = 5;
	private int lvSpeed = 0, lvFireRate = 0, lvShield = 0, lvBomb = 0, lvLaser = 0;

	private static final double SPEED_PER_LV   = 0.10;
	private static final int    FIRE_PER_LV_MS = 40;
	private static final int    SHIELD_BONUS_MS_PER_LV = 250;
	private static final int    BOMB_RADIUS_PER_LV = 20;
	private static final int    LASER_HALF_PER_LV  = 4;

	/* ========================= 상점(스킨) ========================= */
	private static class Skin {
		final String name, spriteRef;
		final int price;
		boolean owned;
		Skin(String n, String ref, int price, boolean owned){this.name=n; this.spriteRef=ref; this.price=price; this.owned=owned;}
	}
	private Skin[] skins = new Skin[] {
			new Skin("Blue",  "sprites/ship_blue.gif",  800, false),
			new Skin("Gold",  "sprites/ship_gold.gif", 1200, false),
			new Skin("Green", "sprites/ship_green.gif", 900,  false)
	};
	private int selectedSkinIndex = 0;
	private int coins = 0;

	// 강화/상점 버튼
	private final MenuButton rfSpeedBtn      = new MenuButton("+이속");
	private final MenuButton rfFireRateBtn   = new MenuButton("+연사");
	private final MenuButton rfBombBtn       = new MenuButton("+폭탄");
	private final MenuButton rfLaserBtn      = new MenuButton("+레이저");
	private final MenuButton rfBackBtn       = new MenuButton("완료");

	private final MenuButton shopBackBtn = new MenuButton("← 뒤로");
	private final MenuButton[] shopBuyBtns   = { new MenuButton("구매"), new MenuButton("구매"), new MenuButton("구매") };
	private final MenuButton[] shopEquipBtns = { new MenuButton("장착"), new MenuButton("장착"), new MenuButton("장착") };

	private static final int COIN_PER_ALIEN = 5;
	private static final int COIN_ON_CLEAR  = 200;

	/* ====== ★ 레벨 튜닝용 상태 (추가) ====== */
	private double dropBombProb  = 0.10; // 10%
	private double dropLaserProb = 0.05; // 5%
	private double alienSpeedScale = 1.00;

	public Game() {
		container = new JFrame("Space Invaders 102");

		JPanel panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
		panel.setLayout(null);

		setBounds(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
		panel.add(this);
		setIgnoreRepaint(true);

		container.pack();
		container.setResizable(false);
		container.setVisible(true);
		container.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		container.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) { saveNow(); }
		});

		addKeyListener(new KeyInputHandler());
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { handleMouseClick(e.getX(), e.getY()); }
		});
		addMouseMotionListener(new MouseMotionAdapter() {});

		createBufferStrategy(2);
		strategy = getBufferStrategy();

		ship1Sprite    = SpriteStore.get().getSprite("sprites/ship.gif");
		ship2Sprite    = SpriteStore.get().getSprite("sprites/ship2.png");
		ship3Sprite    = SpriteStore.get().getSprite("sprites/ship3.png");
		ship3ProSprite = SpriteStore.get().getSprite("sprites/ship3(pro).png");
		heartFullSprite  = SpriteStore.get().getSprite("sprites/full_heart.png");
		heartEmptySprite = SpriteStore.get().getSprite("sprites/empty_heart.png");

		loadSave();

		initEntities();

		playScoreUI = new PlayScoreEntity(this);
		rankingUI   = new RankingScoreEntity(this);
		entities.add(playScoreUI);
		entities.add(rankingUI);
	}

	private void initEntities() {
		ship = new ShipEntity(this, "sprites/ship.gif", 370, 550);
		entities.add(ship);

		int rows = 5, cols = 12;
		int startX = 100, startY = 50, dx = 50, dy = 30;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				Entity alien = new AlienEntity(this, startX + c * dx, startY + r * dy);
				entities.add(alien);
			}
		}
	}

	private void initEntitiesForLevel(int level) {
		String shipImage = "sprites/ship.gif";
		if (selectedSkinIndex >= 0 && selectedSkinIndex < skins.length && skins[selectedSkinIndex].owned) {
			shipImage = skins[selectedSkinIndex].spriteRef;
		}
		ship = new ShipEntity(this, shipImage, 370, 550);
		entities.add(ship);

		int rows = 5, cols = 12;
		int startX = 100, startY = 50, dx = 50, dy = 30;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				Entity alien = new AlienEntity(this, startX + c * dx, startY + r * dy);
				entities.add(alien);
			}
		}
	}

	private void applyShipPreset(int idx) {
		moveSpeed = 300;
		shieldActive = false;
		invulnUntil = 0;
		killsSinceLastShield = 0;

		if (idx == 1)      moveSpeed = 150; // #2 이속 1/2 + 2연발
		else if (idx == 2) moveSpeed = 240; // #3 약간↓ + 30킬 방어막
	}

	private void startGame() {
		entities.clear();
		if (playScoreUI == null) playScoreUI = new PlayScoreEntity(this);
		if (rankingUI   == null) rankingUI   = new RankingScoreEntity(this);

		initEntitiesForLevel(selectedLevel);
		entities.add(playScoreUI);
		entities.add(rankingUI);

		leftPressed = rightPressed = firePressed = false;
		bombPressed = false; laserPressed = false;
		shotsFiredRun = 0;
		runStartTime  = SystemTimer.getTime();
		lastRunElapsedMs = -1;

		score = 0;
		lives = maxLives;
		invulnUntil = 0;
		shieldActive = false;
		killsSinceLastShield = 0;

		bombCount = 0;
		laserCount = 0;

		lastAlienFire = 0;
		nextAsteroidSpawn = 0;
		lastBlackHoleSpawn = 0;
		lastBombFire = 0;
		lastLaserUse = 0;

		boss = null;
		bossSpawned = false;
		bossDefeated = false;

		waitingForKeyPress = false;
		showingLevelSelect = false;
		showingShipSelect  = false;
		showingChallenge   = false;
		showingReinforce   = false;
		showingShop        = false;
		message = "";

		double baseSpeed = moveSpeed;
		moveSpeed = baseSpeed * (1.0 + lvSpeed * 0.10);
		firingInterval = Math.max(100, 500 - lvFireRate * 40);

		/* ★ 레벨 튜닝 적용 */
		applyLevelTuning();
	}

	/* ===== 로직/충돌/알림 ===== */
	public void addEntity(Entity e){ entities.add(e); }
	public void updateLogic() { for (int i=0;i<entities.size();i++) entities.get(i).doLogic(); }
	public void removeEntity(Entity entity) { removeList.add(entity); }
	public void loseHeart() { notifyDeath(); }
	public boolean isPlayerInvincible() { return SystemTimer.getTime() < invulnUntil; }
	public void playerHit() { notifyDeath(); }

	public int getShipCenterX() {
		if (ship == null) return VIRTUAL_WIDTH / 2;
		return ship.getX() + ship.getWidth() / 2;
	}

	public void notifyDeath() {
		long now = SystemTimer.getTime();
		if (selectedShipIndex == 2 && now < invulnUntil) return;

		if (selectedShipIndex == 2 && shieldActive) {
			shieldActive = false;
			invulnUntil  = now + 1000 + lvShield * 250;
			showToast("Block!", 1200);
			return;
		}

		lives = Math.max(0, lives - 1);
		if (lives > 0) {
			long extra = lvShield * 250;
			invulnUntil = now + 1000 + extra;
			showToast("Hit! 남은 목숨: " + lives, 1200);
			return;
		}

		lastRunElapsedMs = (runStartTime == 0) ? -1 : (now - runStartTime);
		message = "Out of lives! Try again?";
		waitingForKeyPress = true;
		showHomeOnMenu = true;

		saveData.totalKillsOverall = Math.max(saveData.totalKillsOverall, totalKills);
		saveNow();
	}

	public void notifyWin() {
		long now = SystemTimer.getTime();
		long elapsed = (runStartTime == 0) ? 0 : (now - runStartTime);
		lastRunElapsedMs = elapsed;

		int bonus = lives * 500;
		int timeBonus = 0;
		if (elapsed > 0) {
			long sec = elapsed / 1000;
			timeBonus = Math.max(0, 1000 - (int)(sec * 10));
		}
		addScore(bonus + timeBonus);

		if (!achClear100 && shotsFiredRun <= 100) { achClear100 = true; showToast("도전과제 달성: 100발 안에 클리어!", 2500); }
		if (!achClear1Min && elapsed <= 60_000)   { achClear1Min = true; showToast("도전과제 달성: 1분 안에 클리어!", 2500); }

		if (selectedLevel >= 1 && selectedLevel < 5) {
			if (!levelUnlocked[selectedLevel]) {
				levelUnlocked[selectedLevel] = true;
				showToast("레벨 " + (selectedLevel + 1) + " 해금!", 1800);
			}
		}

		int rpGain = 2 + Math.max(0, lives / 2);
		reinforcePoints += rpGain;
		saveData.reinforcePoints = reinforcePoints;
		showToast("강화 포인트 +" + rpGain + " (보유: " + reinforcePoints + ")", 1500);

		coins += COIN_ON_CLEAR;
		showToast("코인 +" + COIN_ON_CLEAR + " (보유: " + coins + ")", 1500);

		message = "Well done! You Win!";
		waitingForKeyPress = true;
		showHomeOnMenu = true;

		int highest = 1;
		for (int i = 0; i < 5; i++) if (levelUnlocked[i]) highest = i + 1;
		saveData.highestUnlockedLevel = Math.max(saveData.highestUnlockedLevel, highest);
		if (saveData.bestTimeMs < 0 || elapsed < saveData.bestTimeMs) saveData.bestTimeMs = (int) elapsed;
		saveData.achClear100 = saveData.achClear100 || achClear100;
		saveData.achClear1Min = saveData.achClear1Min || achClear1Min;
		saveData.achKill10 = saveData.achKill10 || achKill10;
		saveData.totalKillsOverall = Math.max(saveData.totalKillsOverall, totalKills);

		submitScoreToLeaderboard(saveData.playerName, score);
		saveNow();
	}

	public void notifyAlienKilled() {
		addScore(SCORE_ALIEN);
		coins += COIN_PER_ALIEN;
		notifyAlienKilledAt(-1, -1);  // 승리 판정/드랍은 별도
	}

	public void notifyAlienKilledAt(int centerX, int centerY) {
		totalKills++;
		killsSinceLastShield++;

		if (!achKill10 && totalKills >= 10) {
			achKill10 = true;
			showToast("도전과제 달성: 적 10마리 격파!", 2000);
			saveData.achKill10 = true;
			saveNow();
		}

		if (selectedShipIndex == 2) {
			if (killsSinceLastShield >= 30 && !shieldActive) {
				shieldActive = true;
				killsSinceLastShield = 0;
				showToast("방어막 획득! (적 30마리 처치)", 2000);
			}
		}

		for (int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			if (e instanceof AlienEntity) e.setHorizontalMovement(e.getHorizontalMovement()*1.02);
		}

		// ★ 레벨별 드랍 확률 사용
		if (centerX >= 0 && rng.nextDouble() < dropBombProb) {
			int spriteHalf = 12;
			spawnBombItemAt(centerX - spriteHalf, centerY - spriteHalf);
		}
		if (centerX >= 0 && rng.nextDouble() < dropLaserProb) {
			int spriteHalf = 12;
			spawnLaserItemAt(centerX - spriteHalf, centerY - spriteHalf);
		}
	}

	private void checkWinCondition() {
		int aliveAliens = 0;
		for (int i = 0; i < entities.size(); i++) {
			if (entities.get(i) instanceof AlienEntity) aliveAliens++;
		}
		if (aliveAliens == 0) {
			if (!bossSpawned) {
				spawnBoss();
				return;
			}
			if (bossDefeated) notifyWin();
		}
	}

	private void spawnBoss() {
		int bx = 400;
		int by = 50;

		BossEntity boss = new BossEntity(this, bx, by);
		entities.add(boss);
		bossSpawned = true;

		// 레벨별 보스 사격 간격(ms)
		long[] BOSS_SHOT_MS_BY_LEVEL = {900, 800, 700, 600, 500}; // 1~5레벨
		int li = Math.max(1, Math.min(5, selectedLevel)) - 1;
		boss.setShotInterval(BOSS_SHOT_MS_BY_LEVEL[li]);

		showToast("보스 등장!", 1500);


}

	public void onBossDefeated(BossEntity b) {
		if (boss == b) {
			bossDefeated = true;
			addScore(2000);
			coins += 300;
			showToast("보스 격파!", 1500);
		}
	}

	/* ===== 발사/아이템 ===== */
	public void tryToFire() {
		if (System.currentTimeMillis() - lastFire < firingInterval) return;
		lastFire = System.currentTimeMillis();

		if (selectedShipIndex == 1) {
			ShotEntity L = new ShotEntity(this, "sprites/shot.gif",
					(int)(ship.getX() + 4),  (int)(ship.getY() - 30));
			ShotEntity R = new ShotEntity(this, "sprites/shot.gif",
					(int)(ship.getX() + 16), (int)(ship.getY() - 30));
			entities.add(L); entities.add(R);
			shotsFiredRun += 2;
		} else {
			ShotEntity shot = new ShotEntity(this, "sprites/shot.gif",
					(int)(ship.getX() + 10), (int)(ship.getY() - 30));
			entities.add(shot);
			shotsFiredRun += 1;
		}
	}

	private void tryToFireBomb() {
		if (bombCount <= 0) { showToast("폭탄이 없습니다!", 800); return; }
		if (System.currentTimeMillis() - lastBombFire < bombFireInterval) return;
		lastBombFire = System.currentTimeMillis();

		int bx = (int) (ship.getX() + ship.getWidth()/2.0 - 8);
		int by = (int) (ship.getY() - 20);
		BombEntity proj = new BombEntity(this, bx, by);
		proj.setMode(BombEntity.Mode.PROJECTILE);
		entities.add(proj);
		bombCount--;
	}

	private void tryToFireLaser() {
		if (laserCount <= 0) { showToast("레이저가 없습니다!", 800); return; }
		if (System.currentTimeMillis() - lastLaserUse < laserCooldown) return;
		lastLaserUse = System.currentTimeMillis();
		laserCount = 0;

		// 레이저 빔 0.5초 유지 (LaserEntity가 매 프레임 tickLaserAt 호출)
		int cx = getShipCenterX();
		entities.add(LaserEntity.createActiveBeam(this, cx, 500));
		showToast("LASER!", 300);
	}

	private Entity pickRandomAlien() {
		ArrayList<Entity> aliens = new ArrayList<>();
		for (int i=0;i<entities.size();i++) if (entities.get(i) instanceof AlienEntity) aliens.add(entities.get(i));
		if (aliens.isEmpty()) return null;
		return aliens.get(rng.nextInt(aliens.size()));
	}

	private void tryAlienFire() {
		long now = SystemTimer.getTime();
		if (now - lastAlienFire < alienFireInterval) return;
		Entity shooter = pickRandomAlien();
		if (shooter == null) return;
		int sx = (int)(shooter.getX() + 12);
		int sy = (int)(shooter.getY() + 20);
		entities.add(new AlienShotEntity(this, "sprites/shot.gif", sx, sy));
		lastAlienFire = now;
	}

	private void spawnAsteroid() {
		int x = rng.nextInt(Math.max(1, VIRTUAL_WIDTH - 32));
		int y = -32;
		double fallSpeed = 500;
		entities.add(new AsteroidEntity(this, x, y, fallSpeed));
	}

	private void spawnBlackHole() {
		int w = 48, h = 48;
		double sx = (ship != null) ? ship.getX() + ship.getWidth() / 2.0 : VIRTUAL_WIDTH / 2.0;
		double sy = (ship != null) ? ship.getY() + ship.getHeight() / 2.0 : VIRTUAL_HEIGHT / 2.0;

		double minD = 120.0, maxD = 220.0;
		double ang  = rng.nextDouble() * Math.PI * 2.0;
		double dist = minD + rng.nextDouble() * (maxD - minD);

		int x = (int) Math.round(sx + Math.cos(ang) * dist) - w / 2;
		int y = (int) Math.round(sy + Math.sin(ang) * dist) - h / 2;

		if (x < 0) x = 0;
		if (y < 0) y = 0;
		if (x > VIRTUAL_WIDTH  - w) x = VIRTUAL_WIDTH  - w;
		if (y > VIRTUAL_HEIGHT - h) y = VIRTUAL_HEIGHT - h;

		entities.add(new BlackHoleEntity(this, x, y, blackHoleRadius, 0.60f, blackHoleLifeMs));
	}

	private void spawnBombItemAt(int x, int y) { entities.add(new BombEntity(this, x, y)); }
	private void spawnLaserItemAt(int x, int y) { entities.add(LaserEntity.createDropItem(this, x, y)); }

	public void activateBombAt(int cx, int cy) {
		int radius = 160 + lvBomb * 20;
		ArrayList<Entity> toRemove = new ArrayList<>();
		for (Entity e : entities) {
			if (e instanceof AlienEntity || e instanceof AlienShotEntity || e instanceof AsteroidEntity) {
				double ex = e.getX() + e.getWidth()/2.0;
				double ey = e.getY() + e.getHeight()/2.0;
				double dx = ex - cx, dy = ey - cy;
				if (dx*dx + dy*dy <= (long)radius*radius) toRemove.add(e);
			} else if (e instanceof BossEntity) {
				double ex = e.getX() + e.getWidth()/2.0;
				double ey = e.getY() + e.getHeight()/2.0;
				double dx = ex - cx, dy = ey - cy;
				if (dx*dx + dy*dy <= (long)radius*radius) {
					((BossEntity)e).takeDamage(80);
				}
			}
		}
		for (Entity e: toRemove) { if (e instanceof AlienEntity) notifyAlienKilled(); removeEntity(e); }
		showToast("BOOM!", 700);
	}

	/** 레이저 지속 판정 (LaserEntity에서 매 프레임 호출) */
	public void tickLaserAt(int cx, int halfWidth) {
		int left = cx - (halfWidth + lvLaser * 4);
		int right = cx + (halfWidth + lvLaser * 4);

		ArrayList<Entity> toRemove = new ArrayList<>();
		for (Entity e : entities) {
			if (e instanceof AlienEntity || e instanceof AlienShotEntity || e instanceof AsteroidEntity) {
				int ex = e.getX() + e.getWidth()/2;
				if (ex >= left && ex <= right) toRemove.add(e);
			} else if (e instanceof BossEntity) {
				int ex = e.getX() + e.getWidth()/2;
				if (ex >= left && ex <= right) {
					((BossEntity)e).takeDamage(10); // 프레임당 누적 데미지
				}
			}
		}
		for (Entity e: toRemove) { if (e instanceof AlienEntity) notifyAlienKilled(); removeEntity(e); }
	}

	public float getBlackHoleSpeedScaleFor(double cx, double cy) {
		for (Entity e : entities) {
			if (e instanceof BlackHoleEntity) {
				BlackHoleEntity bh = (BlackHoleEntity) e;
				double bx = e.getX() + e.getWidth()/2.0, by = e.getY() + e.getHeight()/2.0;
				double dx = bx - cx, dy = by - cy;
				double distSq = dx*dx + dy*dy, r = bh.getRadius();
				if (distSq <= r*r) return 0.5f;
			}
		}
		return 1.0f;
	}

	/* ===== HUD/메뉴/렌더 ===== */
	private void drawLives(Graphics2D g) {
		int x = 10, y = 10, gap = 5;
		for (int i=0;i<maxLives;i++) {
			Sprite heart = (i < lives) ? heartFullSprite : heartEmptySprite;
			if (heart != null) heart.draw(g, x + i * (heart.getWidth() + gap), y);
		}
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		g.setColor(Color.WHITE);
		// 왼쪽 하단으로 이동
		g.drawString("Bomb: " + bombCount + "/" + bombMax + "  (B키)", 10, 560);
		g.drawString("Laser: " + laserCount + "/" + laserMax + "  (L키)", 10, 580);
		g.drawString("RP: " + reinforcePoints, 10, 78);
		g.drawString("Coins: " + coins, 10, 96);
	}

	private void drawBossHP(Graphics2D g) {
		if (boss == null || boss.isDead()) return;
		int cur = boss.getHP();
		int max = boss.getMaxHP();

		int barW = 400, barH = 14;
		int x = (VIRTUAL_WIDTH - barW) / 2;
		int y = 36;

		g.setColor(new Color(0,0,0,160));
		g.fillRoundRect(x-2, y-2, barW+4, barH+4, 8, 8);

		double ratio = Math.max(0, Math.min(1.0, cur / (double) max));
		int fill = (int)(barW * ratio);

		g.setColor(new Color(200, 50, 50));
		g.fillRoundRect(x, y, fill, barH, 8, 8);

		g.setColor(Color.WHITE);
		g.drawString("BOSS", x, y-6);
	}

	private void showToast(String msg, long durationMs) { toastText = msg; toastUntil = SystemTimer.getTime() + durationMs; }

	private void drawToast(Graphics2D g) {
		if (toastText == null || SystemTimer.getTime() > toastUntil) return;
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		FontMetrics fm = g.getFontMetrics();
		int textW = fm.stringWidth(toastText), textH = fm.getHeight();
		int x = (VIRTUAL_WIDTH - textW) / 2, y = VIRTUAL_HEIGHT - 60;
		g.setColor(new Color(0,0,0,150));
		g.fillRoundRect(x - 10, y - textH, textW + 20, textH + 10, 10, 10);
		g.setColor(Color.white); g.drawString(toastText, x, y);
	}

	private void drawCenteredString(Graphics2D g, String text, int width, int y) {
		FontMetrics fm = g.getFontMetrics();
		int x = (width - fm.stringWidth(text)) / 2;
		g.drawString(text, x, y);
	}

	private void layoutBackButton() { backButton.setBounds(20, 20, 140, 40); }

	private void layoutMainMenuButtons(boolean includeHome) {
		int bw = 120, bh = 38;
		int bx = (VIRTUAL_WIDTH - bw) / 2;
		int gap = 12;
		int byPlay = 300;
		playButton.setBounds(bx, byPlay, bw, bh);
		challengeButton.setBounds(bx, byPlay + bh + gap, bw, bh);
		shopButton.setBounds(bx, byPlay + (bh + gap) * 2, bw, bh);
		reinforceButton.setBounds(bx, byPlay + (bh + gap) * 3, bw, bh);
		// resetButton.setBounds(...) 제거
		if (includeHome) homeButton.setBounds(bx, byPlay + (bh + gap) * 5, bw, bh);
	}

	// ✅ 설정 버튼 및 설정 패널 레이아웃
	private void layoutSettingsButtons() {
		// 우상단(여백 20)
		settingsButton.setBounds(VIRTUAL_WIDTH - 100, 20, 80, 40);
		if (showingSettings) {
			// 설정 패널의 '초기화' 버튼
			resetInSettingsBtn.setBounds(VIRTUAL_WIDTH - 140, 70, 120, 40);
		}
	}

	private void layoutLevelButtons() {
		// 메인화면 버튼 스타일과 비슷하게 중앙 세로 배치
		int bw = 160, bh = 44, gap = 12;
		int bx = (VIRTUAL_WIDTH - bw) / 2;
		int startY = 240;

		for (int i = 0; i < 5; i++) {
			levelButtons[i].setBounds(bx, startY + i * (bh + gap), bw, bh);
		}
	}


	private void layoutShipSelectButtons() {
		int centerY = 260;
		int[] xs = { 200, 400, 600 };
		int bw = 160, bh = 44, by = centerY + 60;
		for (int i=0;i<3;i++) selectButtons[i].setBounds(xs[i]-bw/2, by, bw, bh);
	}

	private void drawLevelSelectScreen(Graphics2D g) {
		g.setColor(new Color(20, 20, 20));
		g.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

		layoutBackButton();
		backButton.draw(g, false);

		g.setColor(new Color(220, 220, 220));
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		drawCenteredString(g, "레벨 선택", VIRTUAL_WIDTH, 150);

		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		drawCenteredString(g, "각 레벨을 클리어하면 다음 레벨이 해금됩니다.", VIRTUAL_WIDTH, 180);

		// 버튼 배치 & 그리기 (세로)
		int bw = 160, bh = 44, gap = 12;
		int bx = (VIRTUAL_WIDTH - bw) / 2;
		int startY = 240;

		layoutLevelButtons();
		for (int i = 0; i < 5; i++) {
			levelButtons[i].draw(g, false);
		}

		// 잠금 오버레이(해금되지 않은 버튼 위에 반투명 박스 + '잠김')
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		for (int i = 0; i < 5; i++) {
			if (!levelUnlocked[i]) {
				int x = bx;
				int y = startY + i * (bh + gap);

				g.setColor(new Color(0, 0, 0, 140));
				g.fillRoundRect(x, y, bw, bh, 20, 20);

				g.setColor(new Color(255, 255, 255, 230));
				String lockText = "잠김";
				FontMetrics fm = g.getFontMetrics();
				int tx = x + (bw - fm.stringWidth(lockText)) / 2;
				int ty = y + (bh - fm.getHeight()) / 2 + fm.getAscent();
				g.drawString(lockText, tx, ty);
			}
		}
	}


	private void drawShipSelectScreen(Graphics2D g) {
		g.setColor(new Color(20,20,20)); g.fillRect(0,0,VIRTUAL_WIDTH,VIRTUAL_HEIGHT);
		layoutBackButton(); backButton.draw(g, false);

		g.setColor(new Color(220,220,220));
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		drawCenteredString(g, "기체 선택", VIRTUAL_WIDTH, 140);

		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		drawCenteredString(g, "1) 기본  2) 이속 반 + 2연발  3) 이속 약간↓ + 30킬마다 방어막(1회, 스택X)", VIRTUAL_WIDTH, 170);

		int centerY = 260;
		int[] xs = { 200, 400, 600 };
		for (int i=0;i<3;i++) {
			g.setColor(new Color(255,255,255,25));
			g.fillOval(xs[i]-45, centerY-30, 90, 60);
			Sprite preview = (i==0) ? ship1Sprite : (i==1 ? ship2Sprite : ship3Sprite);
			if (preview != null) preview.draw(g, xs[i]-16, centerY-16);
		}

		layoutShipSelectButtons();
		for (int i=0;i<3;i++) selectButtons[i].draw(g, false);
	}

	private void layoutReinforceButtons() {
		int bw = 100, bh = 48;
		int gap = 22;

		int totalW = bw * 2 + gap;
		int baseX = (VIRTUAL_WIDTH - totalW) / 2;
		int baseY = 240;

		// 2 x 2
		rfSpeedBtn.setBounds   (baseX,             baseY,          bw, bh);
		rfFireRateBtn.setBounds(baseX + bw + gap,  baseY,          bw, bh);
		rfBombBtn.setBounds    (baseX,             baseY + bh+gap, bw, bh);
		rfLaserBtn.setBounds   (baseX + bw + gap,  baseY + bh+gap, bw, bh);

		rfBackBtn.setBounds((VIRTUAL_WIDTH - 200)/2, baseY + (bh+gap)*2 + 30, 200, 52);
	}


	private void drawReinforceScreen(Graphics2D g) {
		g.setColor(new Color(0,0,0,200));
		g.fillRect(0,0,VIRTUAL_WIDTH,VIRTUAL_HEIGHT);

		// 제목/요약
		g.setColor(Color.WHITE);
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		drawCenteredString(g, "강화", VIRTUAL_WIDTH, 150);

		g.setFont(new Font("SansSerif", Font.PLAIN, 16));
		String info = "강화 포인트(RP): " + reinforcePoints + "   (최대 레벨: " + REINF_MAX + ")";
		drawCenteredString(g, info, VIRTUAL_WIDTH, 180);

		// 버튼 배치
		layoutReinforceButtons();
		rfSpeedBtn.draw(g, false);
		rfFireRateBtn.draw(g, false);
		rfBombBtn.draw(g, false);
		rfLaserBtn.draw(g, false);
		rfBackBtn.draw(g, false);

		// 각 버튼 아래 레벨 텍스트
		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		g.setColor(new Color(230,230,230));

		// speed
		{
			java.awt.Rectangle r = new java.awt.Rectangle(rfSpeedBtn.x, rfSpeedBtn.y, rfSpeedBtn.w, rfSpeedBtn.h);
			String t = "Lv " + lvSpeed + " / " + REINF_MAX;
			int tx = r.x + (r.width - g.getFontMetrics().stringWidth(t)) / 2;
			int ty = r.y + r.height + 18;
			g.drawString(t, tx, ty);
		}
		// fire-rate
		{
			java.awt.Rectangle r = new java.awt.Rectangle(rfFireRateBtn.x, rfFireRateBtn.y, rfFireRateBtn.w, rfFireRateBtn.h);
			String t = "Lv " + lvFireRate + " / " + REINF_MAX;
			int tx = r.x + (r.width - g.getFontMetrics().stringWidth(t)) / 2;
			int ty = r.y + r.height + 18;
			g.drawString(t, tx, ty);
		}
		// bomb
		{
			java.awt.Rectangle r = new java.awt.Rectangle(rfBombBtn.x, rfBombBtn.y, rfBombBtn.w, rfBombBtn.h);
			String t = "Lv " + lvBomb + " / " + REINF_MAX;
			int tx = r.x + (r.width - g.getFontMetrics().stringWidth(t)) / 2;
			int ty = r.y + r.height + 18;
			g.drawString(t, tx, ty);
		}
		// laser
		{
			java.awt.Rectangle r = new java.awt.Rectangle(rfLaserBtn.x, rfLaserBtn.y, rfLaserBtn.w, rfLaserBtn.h);
			String t = "Lv " + lvLaser + " / " + REINF_MAX;
			int tx = r.x + (r.width - g.getFontMetrics().stringWidth(t)) / 2;
			int ty = r.y + r.height + 18;
			g.drawString(t, tx, ty);
		}
	}


	private void layoutShopButtons() {
		int startY = 220;
		int rowH = 90;
		int buyX = 520, equipX = 610, btnW = 70, btnH = 36;
		for (int i=0;i<skins.length;i++) {
			int y = startY + i*rowH;
			shopBuyBtns[i].setBounds(buyX,   y, btnW, btnH);
			shopEquipBtns[i].setBounds(equipX, y, btnW, btnH);
		}
		shopBackBtn.setBounds(20, 20, 100, 40);
	}

	private void drawShopScreen(Graphics2D g) {
		g.setColor(new Color(15,15,18));
		g.fillRect(0,0,VIRTUAL_WIDTH,VIRTUAL_HEIGHT);
		shopBackBtn.draw(g, false);

		g.setColor(new Color(230,230,230));
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		drawCenteredString(g, "상점 (스킨)", VIRTUAL_WIDTH, 120);

		g.setFont(new Font("SansSerif", Font.PLAIN, 16));
		drawCenteredString(g, "코인: " + coins + "   (적 처치/클리어로 획득)", VIRTUAL_WIDTH, 150);

		layoutShopButtons();

		int startY = 200;
		int rowH = 90;
		for (int i=0;i<skins.length;i++) {
			int y = startY + i*rowH;

			g.setColor(new Color(255,255,255,20));
			g.fillRoundRect(140, y-10, 520, 70, 16, 16);
			g.setColor(new Color(255,255,255,50));
			g.drawRoundRect(140, y-10, 520, 70, 16, 16);

			// 프리뷰
			Sprite preview = SpriteStore.get().getSprite(skins[i].spriteRef);
			if (preview != null) preview.draw(g, 160, y);

			// 텍스트
			g.setColor(Color.WHITE);
			g.setFont(new Font("SansSerif", Font.BOLD, 16));
			g.drawString(skins[i].name, 220, y+20);
			g.setFont(new Font("SansSerif", Font.PLAIN, 14));
			String status = skins[i].owned ? (i==selectedSkinIndex ? "장착중" : "보유") : ("가격: " + skins[i].price);
			g.drawString(status, 220, y+42);

			// 버튼
			if (!skins[i].owned) shopBuyBtns[i].draw(g, false);
			else                 shopEquipBtns[i].draw(g, false);
		}
	}

	private void drawMainMenuOverlay(Graphics2D g) {
		g.setColor(new Color(0, 0, 0, 170));
		g.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

		g.setColor(Color.WHITE);
		g.setFont(new Font("SansSerif", Font.BOLD, 36));
		String title = "SPACE INVADERS";
		FontMetrics fm = g.getFontMetrics();
		int tx = (VIRTUAL_WIDTH - fm.stringWidth(title)) / 2;
		g.drawString(title, tx, 200);

		// 메인 버튼들
		layoutMainMenuButtons(showHomeOnMenu);
		playButton.draw(g, false);
		challengeButton.draw(g, false);
		shopButton.draw(g, false);
		reinforceButton.draw(g, false);
		// resetButton.draw(g, false); // ❌ 제거
		if (showHomeOnMenu) homeButton.draw(g, false);

		// ✅ 우상단 설정 버튼 + 설정 패널
		layoutSettingsButtons();
		settingsButton.draw(g, false);

		if (showingSettings) {
			// 작은 설정 패널
			g.setColor(new Color(0,0,0,180));
			g.fillRoundRect(VIRTUAL_WIDTH - 160, 60, 150, 70, 12, 12);
			g.setColor(new Color(255,255,255,200));
			g.drawRoundRect(VIRTUAL_WIDTH - 160, 60, 150, 70, 12, 12);

			resetInSettingsBtn.draw(g, false);
		}
	}

	private void handleMouseClick(int mx, int my) {
		if (showingChallenge) {
			layoutBackButton();
			if (backButton.contains(mx, my)) { showingChallenge = false; }
			return;
		}
		if (showingShop) {
			layoutShopButtons();
			if (shopBackBtn.contains(mx, my)) { showingShop = false; return; }
			for (int i=0;i<skins.length;i++) {
				if (!skins[i].owned && shopBuyBtns[i].contains(mx, my)) { tryBuySkin(i); return; }
				if (skins[i].owned && shopEquipBtns[i].contains(mx, my)) {
					selectedSkinIndex = i;
					saveData.selectedSkinIndex = selectedSkinIndex;
					saveNow();
					showToast("스킨 장착: " + skins[i].name, 900);
					return;
				}
			}
			return;
		}
		if (showingReinforce) {
			layoutReinforceButtons();
			if (rfSpeedBtn.contains(mx, my))    { tryUpgrade(() -> lvSpeed++ , lvSpeed); return; }
			if (rfFireRateBtn.contains(mx, my)) { tryUpgrade(() -> lvFireRate++ , lvFireRate); return; }
			if (rfBombBtn.contains(mx, my))     { tryUpgrade(() -> lvBomb++ , lvBomb); return; }
			if (rfLaserBtn.contains(mx, my))    { tryUpgrade(() -> lvLaser++ , lvLaser); return; }
			if (rfBackBtn.contains(mx, my))     { showingReinforce = false; saveNow(); return; }
			return;
		}

		if (showingLevelSelect) {
			layoutBackButton();
			if (backButton.contains(mx, my)) { showingLevelSelect = false; return; }
			layoutLevelButtons();
			for (int i=0;i<5;i++) {
				if (levelButtons[i].contains(mx, my)) {
					if (levelUnlocked[i]) { selectedLevel = i + 1; showingLevelSelect = false; showingShipSelect = true; }
					else showToast("해금되지 않은 레벨입니다!", 1200);
					return;
				}
			}
			return;
		}
		if (showingShipSelect) {
			layoutBackButton();
			if (backButton.contains(mx, my)) { showingShipSelect = false; showingLevelSelect = true; return; }
			layoutShipSelectButtons();
			for (int i=0;i<3;i++) {
				if (selectButtons[i].contains(mx, my)) {
					selectedShipIndex = i; applyShipPreset(i);
					saveData.lastSelectedShipIndex = selectedShipIndex;
					saveNow();
					startGame(); return;
				}
			}
			return;
		}
		if (waitingForKeyPress && !showingLevelSelect && !showingShipSelect && !showingChallenge && !showingReinforce && !showingShop) {
			layoutMainMenuButtons(showHomeOnMenu);
			layoutSettingsButtons(); // ✅ 설정 버튼도 위치 갱신

			// ✅ 설정 버튼 토글
			if (settingsButton.contains(mx, my)) {
				showingSettings = !showingSettings;
				return;
			}
			// ✅ 설정 패널 안의 초기화
			if (showingSettings && resetInSettingsBtn.contains(mx, my)) {
				resetSaveAndRuntime();
				showingSettings = false;
				return;
			}

			// 기존 메인 버튼들
			if (playButton.contains(mx, my)) { showHomeOnMenu = false; showingLevelSelect = true; return; }
			if (challengeButton.contains(mx, my)) { showHomeOnMenu = false; showingChallenge = true; return; }
			if (shopButton.contains(mx, my)) { showHomeOnMenu = false; showingShop = true; return; }
			if (reinforceButton.contains(mx, my)) { showHomeOnMenu = false; showingReinforce = true; return; }
			// if (resetButton.contains(mx, my)) { resetSaveAndRuntime(); return; } // ❌ 제거
			if (showHomeOnMenu && homeButton.contains(mx, my)) {
				showingLevelSelect = false; showingShipSelect = false; showingChallenge = false; showingReinforce = false; showingShop = false;
				selectedLevel = -1; selectedShipIndex = -1; message = "SPACE INVADERS"; showHomeOnMenu = false; return;
			}
		}
	}

	public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();
		while (gameRunning) {
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			lastFpsTime += delta; fps++;
			if (lastFpsTime >= 1000) { container.setTitle(windowTitle + " (FPS: " + fps + ")"); lastFpsTime = 0; fps = 0; }

			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			if (!waitingForKeyPress) {
				for (int i=0;i<entities.size();i++) entities.get(i).move(delta);
			}

			// 렌더 순서: 블랙홀 → 일반 엔티티(레이저BEAM 제외) → 레이저BEAM(맨 앞)
			for (int i=0;i<entities.size();i++)
				if (entities.get(i) instanceof BlackHoleEntity) entities.get(i).draw(g);

			for (int i=0;i<entities.size();i++) {
				Entity e = entities.get(i);
				if (e instanceof BlackHoleEntity) continue;
				if (e instanceof LaserEntity && ((LaserEntity)e).isBeam()) continue;
				e.draw(g);
			}

			for (int i=0;i<entities.size();i++) {
				Entity e = entities.get(i);
				if (e instanceof LaserEntity && ((LaserEntity)e).isBeam()) e.draw(g);
			}

			if (!waitingForKeyPress) {
				drawLives(g);
				drawBossHP(g);
			}

			if (waitingForKeyPress) {
				if (showingLevelSelect) {
					drawLevelSelectScreen(g);
				} else if (showingShipSelect) {
					drawShipSelectScreen(g);
				} else if (showingChallenge) {
					g.setColor(new Color(0, 0, 0, 200));
					g.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
					g.setColor(Color.WHITE);
					g.setFont(new Font("SansSerif", Font.BOLD, 28));
					String t = "도전 과제";
					FontMetrics fm2 = g.getFontMetrics();
					int cx = (VIRTUAL_WIDTH - fm2.stringWidth(t)) / 2;
					g.drawString(t, cx, 120);

					g.setFont(new Font("SansSerif", Font.PLAIN, 18));
					int y = 180;
					g.drawString("• 적 10마리 처치"      + (achKill10   ? " ✅" : ""), 220, y); y += 30;
					g.drawString("• 100발 이하로 클리어" + (achClear100 ? " ✅" : ""), 220, y); y += 30;
					g.drawString("• 1분 안에 클리어"    + (achClear1Min? " ✅" : ""), 220, y);

					layoutBackButton(); backButton.draw(g, false);
				} else if (showingReinforce) {
					drawReinforceScreen(g);
				} else if (showingShop) {
					drawShopScreen(g);
				} else {
					drawMainMenuOverlay(g);
				}
			}

			drawToast(g);

			if (!waitingForKeyPress) {
				tryAlienFire();
				long now = SystemTimer.getTime();
				if (nextAsteroidSpawn == 0) nextAsteroidSpawn = now + asteroidInterval;
				else if (now >= nextAsteroidSpawn) { spawnAsteroid(); nextAsteroidSpawn = now + asteroidInterval; }

				if (lastBlackHoleSpawn == 0) lastBlackHoleSpawn = now + blackHoleInterval;
				else if (now >= lastBlackHoleSpawn) { spawnBlackHole(); lastBlackHoleSpawn = now + blackHoleInterval; }
			}

			if (!waitingForKeyPress) {
				for (int p=0;p<entities.size();p++) {
					for (int s=p+1;s<entities.size();s++) {
						Entity me = entities.get(p), him = entities.get(s);
						if (me.collidesWith(him)) { me.collidedWith(him); him.collidedWith(me); }
					}
				}
			}

			entities.removeAll(removeList); removeList.clear();

			if (!waitingForKeyPress) checkWinCondition();

			if (logicRequiredThisLoop) { for (int i=0;i<entities.size();i++) entities.get(i).doLogic(); logicRequiredThisLoop=false; }

			g.dispose(); strategy.show();

			if (ship != null) ship.setHorizontalMovement(0);
			if (!waitingForKeyPress) {
				double effSpeed = moveSpeed;
				if (ship != null) {
					double cx = ship.getX() + ship.getWidth() / 2.0;
					double cy = ship.getY() + ship.getHeight() / 2.0;
					float scale = getBlackHoleSpeedScaleFor(cx, cy);
					effSpeed *= scale;
				}
				if (leftPressed && !rightPressed)      { if (ship != null) ship.setHorizontalMovement(-effSpeed); }
				else if (rightPressed && !leftPressed) { if (ship != null) ship.setHorizontalMovement(effSpeed); }

				if (firePressed)  tryToFire();
				if (bombPressed)  tryToFireBomb();
				if (laserPressed) tryToFireLaser();
			}

			SystemTimer.sleep(10);
		}
	}

	/* ===== 입력 ===== */
	private class KeyInputHandler extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_LEFT)  leftPressed = true;
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
			if (e.getKeyCode() == KeyEvent.VK_SPACE) firePressed = true;
			if (e.getKeyCode() == KeyEvent.VK_B)     bombPressed = true;
			if (e.getKeyCode() == KeyEvent.VK_L)     laserPressed = true;
		}
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_LEFT)  leftPressed = false;
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
			if (e.getKeyCode() == KeyEvent.VK_SPACE) firePressed = false;
			if (e.getKeyCode() == KeyEvent.VK_B)     bombPressed = false;
			if (e.getKeyCode() == KeyEvent.VK_L)     laserPressed = false;
		}
	}

	/* ===== 간단 버튼 ===== */
	private static class MenuButton {
		private String label; int x, y, w, h; // ← (x,y,w,h) 접근 필요해서 private → package로 변경
		MenuButton(String label) { this.label = label; }
		void setBounds(int x, int y, int w, int h) { this.x=x; this.y=y; this.w=w; this.h=h; }
		boolean contains(int mx, int my) { return (mx>=x && mx<=x+w && my>=y && my<=y+h); }
		void draw(Graphics2D g, boolean hover) {
			g.setColor(hover ? new Color(255,255,255,230) : new Color(255,255,255,200));
			g.fillRoundRect(x, y, w, h, 20, 20);
			g.setColor(new Color(0,0,0,200));
			g.drawRoundRect(x, y, w, h, 20, 20);
			g.setColor(Color.black);
			Font old = g.getFont();
			g.setFont(new Font("SansSerif", Font.BOLD, 20));
			FontMetrics fm = g.getFontMetrics();
			int tx = x + (w - fm.stringWidth(label)) / 2;
			int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
			g.drawString(label, tx, ty);
			g.setFont(old);
		}
	}

	/* ===== 인벤토리/점수/랭킹/저장 ===== */
	public boolean collectBomb() {
		if (bombCount < bombMax) { bombCount++; showToast("폭탄 획득! ("+bombCount+"/"+bombMax+")", 800); return true; }
		else { showToast("폭탄이 가득 찼어요!", 800); return false; }
	}
	public boolean collectLaser() {
		if (laserCount < laserMax) { laserCount = 1; showToast("레이저 획득! (L키로 사용)", 900); return true; }
		else { showToast("레이저는 1개만 소지 가능!", 900); return false; }
	}

	private void addScore(int delta){ if (delta>0) score += delta; }

	private void submitScoreToLeaderboard(String name, int sc){
		if (name == null || name.isEmpty()) name = "PLAYER";
		saveData.leaderboard.add(new RankRow(name, sc));
		saveData.leaderboard.sort((a,b)-> Integer.compare(b.score, a.score));
		while (saveData.leaderboard.size() > LEADERBOARD_MAX) saveData.leaderboard.remove(saveData.leaderboard.size()-1);
		saveNow();
	}

	public int getScore(){ return score; }
	public boolean isWaitingForMenu(){ return waitingForKeyPress; }
	public boolean isShowingChallenge(){ return showingChallenge; }
	public List<RankRow> getTopScores(int limit){
		int n = Math.min(limit, saveData.leaderboard.size());
		return new ArrayList<>(saveData.leaderboard.subList(0, n));
	}

	private void loadSave() {
		java.io.File f = new java.io.File(System.getProperty("user.home", "."), ".space_invaders_save.properties");
		if (!f.exists()) { applySaveToRuntime(); return; }
		java.util.Properties p = new java.util.Properties();
		try (java.io.InputStream in = new java.io.FileInputStream(f)) { p.load(in); }
		catch (Exception e) { applySaveToRuntime(); return; }

		saveData.highestUnlockedLevel = parseInt(p.getProperty("highestUnlockedLevel"), 1);
		saveData.achKill10   = parseBool(p.getProperty("achKill10"));
		saveData.achClear100 = parseBool(p.getProperty("achClear100"));
		saveData.achClear1Min= parseBool(p.getProperty("achClear1Min"));
		saveData.totalKillsOverall = parseInt(p.getProperty("totalKillsOverall"), 0);
		saveData.bestTimeMs = parseInt(p.getProperty("bestTimeMs"), -1);
		saveData.lastSelectedShipIndex = parseInt(p.getProperty("lastSelectedShipIndex"), -1);

		String pn = p.getProperty("playerName");
		if (pn != null && !pn.isEmpty()) saveData.playerName = pn;
		saveData.leaderboard.clear();
		int cnt = parseInt(p.getProperty("leaderboard.count"), 0);
		for (int i=1;i<=cnt;i++) {
			String n = p.getProperty("leaderboard."+i+".name");
			int sc   = parseInt(p.getProperty("leaderboard."+i+".score"), 0);
			if (n != null) saveData.leaderboard.add(new RankRow(n, sc));
		}

		saveData.reinforcePoints = parseInt(p.getProperty("rf.points"), 0);
		saveData.lvSpeed    = parseInt(p.getProperty("rf.lvSpeed"), 0);
		saveData.lvFireRate = parseInt(p.getProperty("rf.lvFireRate"), 0);
		saveData.lvShield   = parseInt(p.getProperty("rf.lvShield"), 0);
		saveData.lvBomb     = parseInt(p.getProperty("rf.lvBomb"), 0);
		saveData.lvLaser    = parseInt(p.getProperty("rf.lvLaser"), 0);

		saveData.coins = parseInt(p.getProperty("coins"), 0);
		saveData.selectedSkinIndex = parseInt(p.getProperty("skin.selected"), 0);
		saveData.ownedSkinsCsv = p.getProperty("skin.owned", "");

		applySaveToRuntime();
	}

	private void saveNow() {
		int highest = 1;
		for (int i=0;i<5;i++) if (levelUnlocked[i]) highest = i + 1;
		saveData.highestUnlockedLevel = Math.max(saveData.highestUnlockedLevel, highest);
		saveData.achKill10   = saveData.achKill10   || achKill10;
		saveData.achClear100 = saveData.achClear100 || achClear100;
		saveData.achClear1Min= saveData.achClear1Min|| achClear1Min;
		saveData.lastSelectedShipIndex = selectedShipIndex;

		java.util.Properties p = new java.util.Properties();
		p.setProperty("highestUnlockedLevel", String.valueOf(saveData.highestUnlockedLevel));
		p.setProperty("achKill10",   String.valueOf(saveData.achKill10));
		p.setProperty("achClear100", String.valueOf(saveData.achClear100));
		p.setProperty("achClear1Min",String.valueOf(saveData.achClear1Min));
		p.setProperty("totalKillsOverall", String.valueOf(saveData.totalKillsOverall));
		p.setProperty("bestTimeMs", String.valueOf(saveData.bestTimeMs));
		p.setProperty("lastSelectedShipIndex", String.valueOf(saveData.lastSelectedShipIndex));

		p.setProperty("playerName", saveData.playerName);
		int cnt = Math.min(saveData.leaderboard.size(), LEADERBOARD_MAX);
		p.setProperty("leaderboard.count", String.valueOf(cnt));
		for (int i=1;i<=cnt;i++) {
			RankRow r = saveData.leaderboard.get(i-1);
			p.setProperty("leaderboard."+i+".name",  r.name);
			p.setProperty("leaderboard."+i+".score", String.valueOf(r.score));
		}

		p.setProperty("rf.points", String.valueOf(reinforcePoints));
		p.setProperty("rf.lvSpeed", String.valueOf(lvSpeed));
		p.setProperty("rf.lvFireRate", String.valueOf(lvFireRate));
		p.setProperty("rf.lvShield", String.valueOf(lvShield));
		p.setProperty("rf.lvBomb", String.valueOf(lvBomb));
		p.setProperty("rf.lvLaser", String.valueOf(lvLaser));

		p.setProperty("coins", String.valueOf(coins));
		p.setProperty("skin.selected", String.valueOf(selectedSkinIndex));
		p.setProperty("skin.owned", ownedSkinsCsv());

		java.io.File f = new java.io.File(System.getProperty("user.home", "."), ".space_invaders_save.properties");
		try (java.io.OutputStream out = new java.io.FileOutputStream(f)) { p.store(out, "Space Invaders Save"); }
		catch (Exception ignored) {}
	}

	private String ownedSkinsCsv() {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<skins.length;i++) {
			if (i>0) sb.append(',');
			sb.append(skins[i].owned ? '1' : '0');
		}
		return sb.toString();
	}

	private void applySaveToRuntime() {
		for (int i=0;i<5;i++) levelUnlocked[i] = false;
		int highest = Math.max(1, Math.min(5, saveData.highestUnlockedLevel));
		for (int i=0;i<highest;i++) levelUnlocked[i] = true;

		achKill10   = saveData.achKill10;
		achClear100 = saveData.achClear100;
		achClear1Min= saveData.achClear1Min;

		if (saveData.lastSelectedShipIndex >= 0 && saveData.lastSelectedShipIndex <= 2) {
			selectedShipIndex = saveData.lastSelectedShipIndex;
		}

		reinforcePoints = saveData.reinforcePoints;
		lvSpeed    = Math.min(REINF_MAX, saveData.lvSpeed);
		lvFireRate = Math.min(REINF_MAX, saveData.lvFireRate);
		lvShield   = Math.min(REINF_MAX, saveData.lvShield);
		lvBomb     = Math.min(REINF_MAX, saveData.lvBomb);
		lvLaser    = Math.min(REINF_MAX, saveData.lvLaser);

		coins = Math.max(0, saveData.coins);
		selectedSkinIndex = Math.max(0, Math.min(saveData.selectedSkinIndex, skins.length-1));
		for (int i=0;i<skins.length;i++) skins[i].owned = false;
		if (saveData.ownedSkinsCsv != null && !saveData.ownedSkinsCsv.isEmpty()) {
			String[] bits = saveData.ownedSkinsCsv.split(",");
			for (int i=0;i<Math.min(bits.length, skins.length); i++) {
				skins[i].owned = "1".equals(bits[i]) || "true".equalsIgnoreCase(bits[i]) || skins[i].owned;
			}
		}
		if (selectedSkinIndex < 0 || selectedSkinIndex >= skins.length) selectedSkinIndex = 0;
	}

	private static boolean parseBool(String s) { return "true".equalsIgnoreCase(s) || "1".equals(s); }
	private static int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }

	private void resetSaveAndRuntime() {
		java.io.File f = new java.io.File(System.getProperty("user.home", "."), ".space_invaders_save.properties");
		if (f.exists()) try { f.delete(); } catch (Exception ignore) {}
		saveData = new SaveState(); applySaveToRuntime();

		selectedLevel = -1; selectedShipIndex = -1; totalKills = 0; shotsFiredRun = 0;
		achKill10 = false; achClear100 = false; achClear1Min = false;
		lives = maxLives; shieldActive = false; invulnUntil = 0; killsSinceLastShield = 0;
		bombCount = 0; laserCount = 0;
		score = 0;

		reinforcePoints = 0;
		lvSpeed = lvFireRate = lvShield = lvBomb = lvLaser = 0;

		coins = 0;
		for (int i=0;i<skins.length;i++) skins[i].owned = false;
		selectedSkinIndex = 0;

		boss = null; bossSpawned = false; bossDefeated = false;

		showToast("저장된 데이터가 초기화되었습니다.", 1400);
	}

	private interface Inc { void go(); }
	private void tryUpgrade(Inc action, int currentLv) {
		if (currentLv >= REINF_MAX) { showToast("이미 최대 레벨입니다.", 900); return; }
		if (reinforcePoints <= 0) { showToast("강화 포인트가 부족합니다.", 900); return; }
		reinforcePoints--;
		action.go();
		saveData.reinforcePoints = reinforcePoints;
		saveData.lvSpeed = lvSpeed; saveData.lvFireRate = lvFireRate; saveData.lvShield = lvShield;
		saveData.lvBomb = lvBomb;   saveData.lvLaser = lvLaser;
		saveNow();
	}

	private void tryBuySkin(int idx) {
		if (idx < 0 || idx >= skins.length) return;
		Skin s = skins[idx];
		if (s.owned) { showToast("이미 보유한 스킨입니다.", 900); return; }
		if (coins < s.price) { showToast("코인이 부족합니다.", 900); return; }
		coins -= s.price;
		s.owned = true;
		selectedSkinIndex = idx; // 구매 즉시 장착
		saveData.selectedSkinIndex = selectedSkinIndex;
		saveNow();
		showToast("구매/장착 완료: " + s.name + " (잔액: "+coins+")", 1300);
	}

	/* ===== ★ 레벨별 튜닝 로직 (추가) ===== */
	private void applyLevelTuning() {
		// 기본값(안전)
		dropBombProb = 0.10;
		dropLaserProb = 0.05;
		alienFireInterval = 1200;
		alienSpeedScale = 1.00;

		switch (selectedLevel) {
			case 1:
				dropBombProb = 0.10; dropLaserProb = 0.06;
				alienFireInterval = 1300;
				alienSpeedScale = 0.80;
				break;
			case 2:
				dropBombProb = 0.08; dropLaserProb = 0.05;
				alienFireInterval = 1200;
				alienSpeedScale = 0.90;
				break;
			case 3:
				dropBombProb = 0.07; dropLaserProb = 0.04;
				alienFireInterval = 1100;
				alienSpeedScale = 1.00;
				break;
			case 4:
				dropBombProb = 0.06; dropLaserProb = 0.03;
				alienFireInterval = 1000;
				alienSpeedScale = 1.10;
				break;
			case 5:
				dropBombProb = 0.05; dropLaserProb = 0.02;
				alienFireInterval = 900;
				alienSpeedScale = 1.20;
				break;
			default:
				break;
		}

		// 현재 필드의 모든 Alien 수평속도에 배율 적용 (AlienEntity는 수정하지 않는 조건)
		for (int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			if (e instanceof AlienEntity) {
				e.setHorizontalMovement(e.getHorizontalMovement() * alienSpeedScale);
			}
		}
	}

	public static void main(String[] args) { Game g = new Game(); g.gameLoop(); }
}
