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
import java.awt.image.BufferStrategy;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

// ✅ 새로 추가된 UI 엔티티
import org.newdawn.spaceinvaders.entity.MainMenuEntity;
import org.newdawn.spaceinvaders.entity.ChallengeMenuEntity;

/**
 * Flow:
 *   Main → Play → [레벨 선택 1~5] → [기체 선택 3종] → 게임 시작
 * Level:
 *   - 최초엔 레벨 1만 해금, 클리어하면 (n+1) 해금
 * Other:
 *   - 도전과제: 적 10마리 / 100발 안 / 1분 안 클리어
 *   - Ship#1 기본 / Ship#2 이속 반 + 2연발 / Ship#3 이속 약간↓ + 30킬마다 방어막(1회, 스택X)
 *   - 각 서브화면(레벨/기체/도전과제)에 "← 돌아가기" 버튼
 *   - 스테이지 종료 후 메뉴에서만 "메인화면" 버튼 노출
 *   - 기체별 스프라이트 연동
 *
 * Win Condition:
 *   - 프레임 말(엔티티 제거 반영 후)에 실제 남은 AlienEntity가 0일 때만 클리어.
 *
 * UI 구조 변경:
 *   - 메인 화면, 도전과제 화면을 각각 Entity로 분리(MainMenuEntity / ChallengeMenuEntity)
 *   - Game은 상태에 맞춰 해당 UI 엔티티를 엔티티 목록에 추가/제거만 수행
 */
public class Game extends Canvas {
	/* ===== Core / Loop ===== */
	private BufferStrategy strategy;
	private boolean gameRunning = true;

	/* ===== Entities ===== */
	private ArrayList entities = new ArrayList();
	private ArrayList removeList = new ArrayList();
	private Entity ship;

	/* ===== Control / Timing ===== */
	private double moveSpeed = 300;     // 기본 이속 (기체 선택에 따라 조정)
	private long lastFire = 0;
	private long firingInterval = 500;

	/* ===== UI State ===== */
	private String message = "SPACE INVADERS";
	private boolean waitingForKeyPress = true;
	private boolean leftPressed = false;
	private boolean rightPressed = false;
	private boolean firePressed = false;
	private boolean logicRequiredThisLoop = false;
	private long lastFpsTime;
	private int fps;
	private String windowTitle = "Space Invaders 102";
	private JFrame container;

	/* ===== Main Menu Buttons ===== */
	private final MenuButton playButton = new MenuButton("Play");
	private final MenuButton challengeButton = new MenuButton("도전과제");
	private final MenuButton homeButton = new MenuButton("메인화면"); // 스테이지 종료 후에만 노출
	private boolean mouseInPlay = false;
	private boolean mouseInChallenge = false;
	private boolean mouseInHome = false;
	private boolean showHomeOnMenu = false; // 스테이지 종료 후 메뉴에서만 true

	/* ===== Common Back Button ===== */
	private final MenuButton backButton = new MenuButton("← 돌아가기");
	private boolean mouseInBack = false;

	/* ===== Level Select ===== */
	private boolean showingLevelSelect = false;
	private final MenuButton[] levelButtons = {
			new MenuButton("Level 1"),
			new MenuButton("Level 2"),
			new MenuButton("Level 3"),
			new MenuButton("Level 4"),
			new MenuButton("Level 5")
	};
	private final boolean[] mouseInLevel = new boolean[5];
	private final boolean[] levelUnlocked = new boolean[5]; // [0]=L1 ... [4]=L5
	private int selectedLevel = -1; // 1~5

	/* ===== Ship Select ===== */
	private boolean showingShipSelect = false;
	private final MenuButton[] selectButtons = {
			new MenuButton("선택하기"),
			new MenuButton("선택하기"),
			new MenuButton("선택하기")
	};
	private final boolean[] mouseInSelect = new boolean[3];
	private int selectedShipIndex = -1; // 0=기본, 1=2연발, 2=방어막

	/* ===== Sprites ===== */
	private Sprite ship1Sprite;      // 기본
	private Sprite ship2Sprite;      // 2번 기체
	private Sprite ship3Sprite;      // 3번 기체(평상시)
	private Sprite ship3ProSprite;   // 3번 기체(방어막 ON)

	/* ===== Challenge Screen ===== */
	private boolean showingChallenge = false;

	/* ===== Achievements ===== */
	private int totalKills = 0;
	private boolean achKill10 = false;

	private int shotsFiredRun = 0;
	private boolean achClear100 = false;

	private long runStartTime = 0;
	private long lastRunElapsedMs = -1;
	private boolean achClear1Min = false;

	/* ===== Ship #3 Shield ===== */
	private boolean shieldActive = false;   // 방어막 보유 여부
	private long invulnUntil = 0;          // 방어막 발동 후 무적 종료 시각(ms)
	private int killsSinceLastShield = 0;  // 30킬마다 방어막 지급(스택X)

	/* ===== Toast ===== */
	private String toastText = null;
	private long toastUntil = 0;

	/* ===== NEW: UI Entities ===== */
	private MainMenuEntity mainMenuEntity;
	private ChallengeMenuEntity challengeEntity;

	public Game() {
		container = new JFrame("Space Invaders 102");

		JPanel panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(800, 600));
		panel.setLayout(null);

		setBounds(0, 0, 800, 600);
		panel.add(this);
		setIgnoreRepaint(true);

		container.pack();
		container.setResizable(false);
		container.setVisible(true);
		container.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		addKeyListener(new KeyInputHandler());

		/* Mouse Input */
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// 도전과제 화면
				if (showingChallenge) {
					if (backButton.contains(e.getX(), e.getY())) {
						showingChallenge = false; // 메인으로
						syncUIEntities();
					}
					return;
				}

				// 레벨 선택 화면
				if (showingLevelSelect) {
					layoutBackButton();
					if (backButton.contains(e.getX(), e.getY())) {
						showingLevelSelect = false; // 메인으로
						syncUIEntities();
						return;
					}
					for (int i = 0; i < 5; i++) {
						if (levelButtons[i].contains(e.getX(), e.getY())) {
							if (levelUnlocked[i]) {
								selectedLevel = i + 1;        // 1..5
								showingLevelSelect = false;
								showingShipSelect = true;      // 다음 단계: 기체 선택
							} else {
								showToast("해금되지 않은 레벨입니다!", 1200);
							}
							syncUIEntities();
							return;
						}
					}
					return;
				}

				// 기체 선택 화면
				if (showingShipSelect) {
					layoutBackButton();
					if (backButton.contains(e.getX(), e.getY())) {
						showingShipSelect = false;
						showingLevelSelect = true;
						syncUIEntities();
						return;
					}
					for (int i = 0; i < 3; i++) {
						if (selectButtons[i].contains(e.getX(), e.getY())) {
							selectedShipIndex = i;
							applyShipPreset(i);
							startGame();                // 본게임 시작
							waitingForKeyPress = false;
							showingShipSelect = false;
							message = "";
							requestFocus();
							syncUIEntities();          // 메뉴 UI 제거
							return;
						}
					}
					return;
				}

				// 기본 메뉴 (메인 오버레이: Play/도전과제/메인화면(조건부))
				if (waitingForKeyPress && !showingLevelSelect && !showingShipSelect && !showingChallenge) {
					layoutMainMenuButtons(showHomeOnMenu); // 좌표 세팅
					if (playButton.contains(e.getX(), e.getY())) {
						showHomeOnMenu = false;
						showingLevelSelect = true;
						syncUIEntities(); // 메인 엔티티 숨김
						return;
					}
					if (challengeButton.contains(e.getX(), e.getY())) {
						showHomeOnMenu = false;
						showingChallenge = true;
						syncUIEntities(); // 메인→도전과제로 전환
						return;
					}
					if (showHomeOnMenu && homeButton.contains(e.getX(), e.getY())) {
						// 완전 메인 상태로
						showingLevelSelect = false;
						showingShipSelect  = false;
						showingChallenge   = false;
						selectedLevel = -1;
						selectedShipIndex = -1;
						message = "SPACE INVADERS";
						showHomeOnMenu = false; // 초기 메인에선 숨김
						syncUIEntities();
						return;
					}
				}
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				mouseInBack = false;
				mouseInHome = false;

				if (!waitingForKeyPress) {
					resetHovers();
					return;
				}
				if (showingChallenge) {
					layoutBackButton();
					mouseInBack = backButton.contains(e.getX(), e.getY());
					resetHoversExceptBack();
					return;
				}
				if (showingLevelSelect) {
					layoutBackButton();
					mouseInBack = backButton.contains(e.getX(), e.getY());
					layoutLevelButtons();
					for (int i = 0; i < 5; i++) {
						mouseInLevel[i] = levelButtons[i].contains(e.getX(), e.getY());
					}
					mouseInPlay = mouseInChallenge = false;
					for (int i = 0; i < 3; i++) mouseInSelect[i] = false;
					return;
				}
				if (showingShipSelect) {
					layoutBackButton();
					mouseInBack = backButton.contains(e.getX(), e.getY());
					layoutShipSelectButtons();
					for (int i = 0; i < 3; i++) {
						mouseInSelect[i] = selectButtons[i].contains(e.getX(), e.getY());
					}
					mouseInPlay = mouseInChallenge = false;
					for (int i = 0; i < 5; i++) mouseInLevel[i] = false;
					return;
				}
				// 기본 메뉴
				layoutMainMenuButtons(showHomeOnMenu);
				mouseInPlay = playButton.contains(e.getX(), e.getY());
				mouseInChallenge = challengeButton.contains(e.getX(), e.getY());
				mouseInHome = showHomeOnMenu && homeButton.contains(e.getX(), e.getY());
				for (int i = 0; i < 5; i++) mouseInLevel[i] = false;
				for (int i = 0; i < 3; i++) mouseInSelect[i] = false;
			}
			private void resetHovers() {
				mouseInPlay = mouseInChallenge = mouseInHome = false;
				for (int i = 0; i < 5; i++) mouseInLevel[i] = false;
				for (int i = 0; i < 3; i++) mouseInSelect[i] = false;
			}
			private void resetHoversExceptBack() {
				mouseInPlay = mouseInChallenge = mouseInHome = false;
				for (int i = 0; i < 5; i++) mouseInLevel[i] = false;
				for (int i = 0; i < 3; i++) mouseInSelect[i] = false;
			}
		});

		requestFocus();

		createBufferStrategy(2);
		strategy = getBufferStrategy();

		/* 스프라이트 로드 (확장자는 실제 파일과 일치시켜야 함) */
		ship1Sprite    = SpriteStore.get().getSprite("sprites/ship.gif");
		ship2Sprite    = SpriteStore.get().getSprite("sprites/ship2.png");
		ship3Sprite    = SpriteStore.get().getSprite("sprites/ship3.png");
		ship3ProSprite = SpriteStore.get().getSprite("sprites/ship3(pro).png");

		// 레벨 해금 초기화: L1만 열려
		for (int i = 0; i < 5; i++) levelUnlocked[i] = false;
		levelUnlocked[0] = true;

		// UI 엔티티 생성
		mainMenuEntity = new MainMenuEntity(this);
		challengeEntity = new ChallengeMenuEntity(this);

		initEntities();
		syncUIEntities(); // 현재 상태에 맞춰 UI 엔티티 add/remove
	}

	/* ===== 기체 프리셋 ===== */
	private void applyShipPreset(int idx) {
		// 기본값 리셋
		moveSpeed = 300;
		shieldActive = false;
		invulnUntil = 0;
		killsSinceLastShield = 0;

		if (idx == 1) {
			// #2: 이속 반, 2연발
			moveSpeed = 150;
		} else if (idx == 2) {
			// #3: 이속 약간 감소 + 방어막(30킬마다 1개, 스택X)
			moveSpeed = 240;
		}
	}

	/* ===== 게임 시작 ===== */
	private void startGame() {
		entities.clear();
		initEntitiesForLevel(selectedLevel); // 레벨별 초기화

		leftPressed = rightPressed = firePressed = false;

		shotsFiredRun = 0;
		runStartTime = SystemTimer.getTime();
		lastRunElapsedMs = -1;

		// 방어막/무적 초기화
		invulnUntil = 0;
		if (selectedShipIndex == 2) {
			shieldActive = false;     // 시작 시 방어막 없음
			killsSinceLastShield = 0; // 30킬마다 지급
		} else {
			shieldActive = false;
		}

		toastText = null; toastUntil = 0;

		// 게임 시작 시 UI 엔티티 제거
		syncUIEntities();
	}

	/* 기본 엔티티 초기화(레벨 공통) */
	private void initEntities() {
		ship = new ShipEntity(this, "sprites/ship.gif", 370, 550);
		entities.add(ship);

		// 기본 외계인 배치 (5 x 12 = 60)
		for (int row = 0; row < 5; row++) {
			for (int x = 0; x < 12; x++) {
				Entity alien = new AlienEntity(this, 100 + (x * 50), 50 + row * 30);
				entities.add(alien);
			}
		}
	}

	/* 레벨별 초기화(현재는 동일 배치, 필요시 차별화 가능) */
	private void initEntitiesForLevel(int level) {
		// 선택된 기체에 맞는 이미지로 플레이어 생성
		String shipImage = "sprites/ship.gif";
		if (selectedShipIndex == 1)      shipImage = "sprites/ship2.png";
		else if (selectedShipIndex == 2) shipImage = "sprites/ship3.png";

		ship = new ShipEntity(this, shipImage, 370, 550);
		entities.add(ship);

		// 외계인들
		int rows = 5;
		int cols = 12;
		int startX = 100, startY = 50, dx = 50, dy = 30;

		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				Entity alien = new AlienEntity(this, startX + c * dx, startY + r * dy);
				entities.add(alien);
			}
		}
	}

	/* ===== Logic hooks ===== */
	public void updateLogic() {
		for (int i = 0; i < entities.size(); i++) {
			((Entity) entities.get(i)).doLogic();
		}
	}

	public void removeEntity(Entity entity) { removeList.add(entity); }

	/* 사망 처리: #3 방어막/무적 고려 */
	public void notifyDeath() {
		long now = SystemTimer.getTime();

		// #3 무적 중이면 무시
		if (selectedShipIndex == 2 && now < invulnUntil) return;

		// #3 방어막 보유 시 소모 + 1초 무적, 죽음 무효
		if (selectedShipIndex == 2 && shieldActive) {
			shieldActive = false;
			invulnUntil = now + 1000;
			showToast("방어막이 공격을 막아냈다! (1초 무적)", 1500);
			return;
		}

		lastRunElapsedMs = (runStartTime == 0) ? -1 : (now - runStartTime);
		message = "Oh no! They got you, try again?";
		waitingForKeyPress = true;
		showHomeOnMenu = true; // 스테이지 종료 후에만 메인화면 버튼 노출

		syncUIEntities(); // 메인 메뉴 엔티티 표시
	}

	/* 승리 처리: 업적 + 다음 레벨 해금 */
	public void notifyWin() {
		long now = SystemTimer.getTime();
		long elapsed = (runStartTime == 0) ? 0 : (now - runStartTime);
		lastRunElapsedMs = elapsed;

		// 업적
		if (!achClear100 && shotsFiredRun <= 100) {
			achClear100 = true;
			showToast("도전과제 달성: 100발 안에 클리어!", 2500);
		}
		if (!achClear1Min && elapsed <= 60_000) {
			achClear1Min = true;
			showToast("도전과제 달성: 1분 안에 클리어!", 2500);
		}

		// 다음 레벨 해금
		if (selectedLevel >= 1 && selectedLevel < 5) {
			if (!levelUnlocked[selectedLevel]) { // 예: L1 클리어 → index 1(L2) 해금
				levelUnlocked[selectedLevel] = true;
				showToast("레벨 " + (selectedLevel + 1) + " 해금!", 1800);
			}
		}

		message = "Well done! You Win!";
		waitingForKeyPress = true;
		showHomeOnMenu = true; // 스테이지 종료 후에만 메인화면 버튼 노출

		syncUIEntities(); // 메인 메뉴 엔티티 표시
	}

	/* 외계인 처치 (속도증가 등만 처리 — 승리체크는 프레임 말 제거 후) */
	public void notifyAlienKilled() {
		totalKills++;
		killsSinceLastShield++;

		// 10마리 처치 업적
		if (!achKill10 && totalKills >= 10) {
			achKill10 = true;
			showToast("도전과제 달성: 적 10마리 격파!", 2000);
		}

		// #3: 30킬마다 방어막 1개 지급(스택X)
		if (selectedShipIndex == 2) {
			if (killsSinceLastShield >= 30 && !shieldActive) {
				shieldActive = true;
				killsSinceLastShield = 0;
				showToast("방어막 획득! (적 30마리 처치)", 2000);
			}
		}

		// 남은 에일리언 속도 2% 증가
		for (int i = 0; i < entities.size(); i++) {
			Entity entity = (Entity) entities.get(i);
			if (entity instanceof AlienEntity) {
				entity.setHorizontalMovement(entity.getHorizontalMovement() * 1.02);
			}
		}

		// (여기서는 승리 체크 X)
	}

	/** 현재 남아있는 외계인 수를 직접 세서 승리 판정 */
	private void checkWinCondition() {
		int alive = 0;
		for (int i = 0; i < entities.size(); i++) {
			if (entities.get(i) instanceof org.newdawn.spaceinvaders.entity.AlienEntity) {
				alive++;
			}
		}
		if (alive == 0) {
			notifyWin();
		}
	}

	/* 발사: #2는 2연발 */
	public void tryToFire() {
		if (System.currentTimeMillis() - lastFire < firingInterval) return;

		lastFire = System.currentTimeMillis();

		if (selectedShipIndex == 1) {
			ShotEntity shotL = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 4,  ship.getY() - 30);
			ShotEntity shotR = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 16, ship.getY() - 30);
			entities.add(shotL);
			entities.add(shotR);
			shotsFiredRun += 2;
		} else {
			ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", ship.getX() + 10, ship.getY() - 30);
			entities.add(shot);
			shotsFiredRun += 1;
		}
	}

	/* ===== Main Loop ===== */
	public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();

		while (gameRunning) {
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			lastFpsTime += delta; fps++;
			if (lastFpsTime >= 1000) {
				container.setTitle(windowTitle + " (FPS: " + fps + ")");
				lastFpsTime = 0; fps = 0;
			}

			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, 800, 600);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			// 움직임
			if (!waitingForKeyPress) {
				for (int i = 0; i < entities.size(); i++) {
					((Entity) entities.get(i)).move(delta);
				}
			}

			// 그리기
			for (int i = 0; i < entities.size(); i++) {
				((Entity) entities.get(i)).draw(g);
			}

			// 방어막 ON일 때 3번 기체 pro 스프라이트 덮어그리기
			if (!waitingForKeyPress && selectedShipIndex == 2 && shieldActive && ship3ProSprite != null && ship != null) {
				ship3ProSprite.draw(g, (int) ship.getX(), (int) ship.getY());
			}

			// HUD: #3 방어막 상태
			if (!waitingForKeyPress && selectedShipIndex == 2) {
				g.setFont(new Font("SansSerif", Font.PLAIN, 14));
				g.setColor(Color.white);
				String shieldStr = "Shield: " + (shieldActive ? "ON" : "OFF");
				g.drawString(shieldStr, 640, 24);
			}

			// 토스트
			drawToast(g);

			// 오버레이 (메뉴 상태)
			if (waitingForKeyPress) {
				// 메인/도전과제는 UI 엔티티가 그린다.
				if (showingLevelSelect)      drawLevelSelectScreen(g);
				else if (showingShipSelect)  drawShipSelectScreen(g);
				// else: main/challenge는 엔티티로 처리
			}

			// 충돌
			if (!waitingForKeyPress) {
				for (int p = 0; p < entities.size(); p++) {
					for (int s = p + 1; s < entities.size(); s++) {
						Entity me = (Entity) entities.get(p);
						Entity him = (Entity) entities.get(s);
						if (me.collidesWith(him)) {
							me.collidedWith(him);
							him.collidedWith(me);
						}
					}
				}
			}

			// 엔티티 제거
			entities.removeAll(removeList);
			removeList.clear();

			// ✅ 제거가 반영된 뒤에 승리 조건 검사
			if (!waitingForKeyPress) {
				checkWinCondition();
			}

			// 로직 플래그 처리
			if (logicRequiredThisLoop) {
				for (int i = 0; i < entities.size(); i++) {
					((Entity) entities.get(i)).doLogic();
				}
				logicRequiredThisLoop = false;
			}

			g.dispose();
			strategy.show();

			// 입력 처리
			if (ship != null) ship.setHorizontalMovement(0);
			if (!waitingForKeyPress) {
				if (leftPressed && !rightPressed)  { if (ship != null) ship.setHorizontalMovement(-moveSpeed); }
				else if (rightPressed && !leftPressed) { if (ship != null) ship.setHorizontalMovement(moveSpeed); }
				if (firePressed) tryToFire();
			}

			SystemTimer.sleep(10);
		}
	}

	/* ===== UI 엔티티 add/remove 동기화 ===== */
	private void syncUIEntities() {
		boolean inMainMenu = waitingForKeyPress && !showingLevelSelect && !showingShipSelect && !showingChallenge;
		boolean inChallenge = waitingForKeyPress && showingChallenge;

		ensureEntity(mainMenuEntity, inMainMenu);
		ensureEntity(challengeEntity, inChallenge);
	}

	private void ensureEntity(Entity ui, boolean shouldBePresent) {
		boolean present = entities.contains(ui);
		if (shouldBePresent && !present) {
			entities.add(ui);
		} else if (!shouldBePresent && present) {
			removeList.add(ui); // 프레임 말에 제거
		}
	}

	/* ===== UI: Main Menu (그리기 루틴을 엔티티에서 호출할 수 있도록 public) ===== */
	public void renderMainMenu(Graphics2D g) {
		g.setColor(new Color(0, 0, 0, 170));
		g.fillRect(0, 0, 800, 600);

		g.setColor(Color.white);
		g.setFont(new Font("SansSerif", Font.BOLD, 36));
		drawCenteredString(g, message != null && !message.isEmpty() ? message : "SPACE INVADERS", 800, 220);

		layoutMainMenuButtons(showHomeOnMenu);
		playButton.draw(g, mouseInPlay);
		challengeButton.draw(g, mouseInChallenge);
		if (showHomeOnMenu) {
			homeButton.draw(g, mouseInHome);
		}

		// 도전과제 배지
		int r = 10;
		int bw = 200, bh = 56, bx = (800 - bw) / 2;
		int byPlay, gap, byChallenge;
		if (showHomeOnMenu) { byPlay = 300; gap = 12; }
		else                { byPlay = 320; gap = 14; }
		byChallenge = byPlay + bh + gap;

		int badgeY = byChallenge + 8, badgeX = bx + bw - r - 8;
		if (achKill10)   { g.setColor(new Color(0,200,100)); g.fillOval(badgeX, badgeY, r, r); badgeX -= (r+8); }
		if (achClear100) { g.setColor(new Color(0,160,255)); g.fillOval(badgeX, badgeY, r, r); badgeX -= (r+8); }
		if (achClear1Min){ g.setColor(new Color(255,180,0)); g.fillOval(badgeX, badgeY, r, r); }
	}

	/* 버튼 좌표 계산: 메인 메뉴용 */
	private void layoutMainMenuButtons(boolean includeHome) {
		int bw = 200, bh = 56;
		int bx = (800 - bw) / 2;

		if (includeHome) {
			int byPlay = 300, gap = 12;
			int byChallenge = byPlay + bh + gap;
			int byHome      = byChallenge + bh + gap;
			playButton.setBounds(bx, byPlay, bw, bh);
			challengeButton.setBounds(bx, byChallenge, bw, bh);
			homeButton.setBounds(bx, byHome, bw, bh);
		} else {
			int byPlay = 320, gap = 14;
			int byChallenge = byPlay + bh + gap;
			playButton.setBounds(bx, byPlay, bw, bh);
			challengeButton.setBounds(bx, byChallenge, bw, bh);
			// homeButton 배치 안 함
		}
	}

	/* ===== UI: Level Select ===== */
	private void drawLevelSelectScreen(Graphics2D g) {
		g.setColor(new Color(20, 20, 20));
		g.fillRect(0, 0, 800, 600);

		// Back
		layoutBackButton();
		backButton.draw(g, mouseInBack);

		g.setColor(new Color(220, 220, 220));
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		drawCenteredString(g, "레벨 선택", 800, 150);

		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		drawCenteredString(g, "처음에는 레벨 1만 해금됩니다. 각 레벨을 클리어하면 다음 레벨이 해금됩니다.", 800, 180);

		layoutLevelButtons();
		int bw = 160, bh = 48;
		int y1 = 260, y2 = y1 + 70;
		int[] xs = { 180, 400, 620, 280, 520 };
		int[] ys = { y1,  y1,  y1,  y2,  y2 };

		for (int i = 0; i < 5; i++) {
			levelButtons[i].draw(g, mouseInLevel[i]);

			// 잠김 오버레이
			if (!levelUnlocked[i]) {
				g.setColor(new Color(0, 0, 0, 140));
				g.fillRoundRect(xs[i] - bw/2, ys[i], bw, bh, 20, 20);
				g.setColor(new Color(255, 255, 255, 230));
				g.setFont(new Font("SansSerif", Font.BOLD, 14));
				String lockText = "잠김";
				FontMetrics fm = g.getFontMetrics();
				int tx = xs[i] - fm.stringWidth(lockText)/2;
				int ty = ys[i] + (bh - fm.getHeight())/2 + fm.getAscent();
				g.drawString(lockText, tx, ty);
			}
		}

		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		drawCenteredString(g, "플레이할 레벨을 선택하세요", 800, y2 + 80);
	}

	private void layoutLevelButtons() {
		int bw = 160, bh = 48;
		int y1 = 260, y2 = y1 + 70;
		int[] xs = { 180, 400, 620, 280, 520 };
		int[] ys = { y1,  y1,  y1,  y2,  y2 };
		for (int i = 0; i < 5; i++) {
			levelButtons[i].setBounds(xs[i] - bw/2, ys[i], bw, bh);
		}
	}

	/* ===== UI: Ship Select ===== */
	private void drawShipSelectScreen(Graphics2D g) {
		g.setColor(new Color(20, 20, 20));
		g.fillRect(0, 0, 800, 600);

		// Back
		layoutBackButton();
		backButton.draw(g, mouseInBack);

		g.setColor(new Color(220, 220, 220));
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		drawCenteredString(g, "기체 선택", 800, 140);

		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		drawCenteredString(g, "1) 기본  2) 이속 반 + 2연발  3) 이속 약간↓ + 30킬마다 방어막(1회, 스택X)", 800, 170);

		int centerY = 260;
		int[] xs = { 200, 400, 600 };

		for (int i = 0; i < 3; i++) {
			Sprite preview = (i == 0) ? ship1Sprite : (i == 1 ? ship2Sprite : ship3Sprite);
			if (preview != null) {
				g.setColor(new Color(255, 255, 255, 25));
				g.fillOval(xs[i] - 45, centerY - 30, 90, 60);
				preview.draw(g, xs[i] - 16, centerY - 16);
			} else {
				g.setColor(new Color(180, 180, 180));
				g.fillRoundRect(xs[i] - 20, centerY - 14, 40, 28, 8, 8);
			}
		}

		layoutShipSelectButtons();
		for (int i = 0; i < 3; i++) {
			selectButtons[i].draw(g, mouseInSelect[i]);
		}

		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		drawCenteredString(g, "원하는 기체의 ‘선택하기’를 누르면 게임이 시작됩니다", 800, centerY + 60 + 44 + 80);
	}

	private void layoutShipSelectButtons() {
		int centerY = 260;
		int[] xs = { 200, 400, 600 };
		int bw = 160, bh = 44, by = centerY + 60;
		for (int i = 0; i < 3; i++) {
			selectButtons[i].setBounds(xs[i] - bw/2, by, bw, bh);
		}
	}

	/* ===== UI: Challenge Screen (엔티티에서 호출) ===== */
	public void renderChallengeScreen(Graphics2D g) {
		g.setColor(new Color(30, 30, 30));
		g.fillRect(0, 0, 800, 600);

		// Back
		layoutBackButton();
		backButton.draw(g, mouseInBack);

		g.setColor(new Color(220, 220, 220));
		g.setFont(new Font("SansSerif", Font.BOLD, 28));
		drawCenteredString(g, "도전과제", 800, 160);

		int y = 220;

		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		drawCenteredString(g, "• 적 10마리 처치", 800, y);
		g.setFont(new Font("SansSerif", Font.PLAIN, 16));
		if (achKill10) drawCenteredString(g, "✅ 적 열마리 죽임 달성!", 800, y + 25);
		else           drawCenteredString(g, "진행도: " + totalKills + " / 10", 800, y + 25);

		y += 70;
		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		drawCenteredString(g, "• 100발 안에 클리어", 800, y);
		g.setFont(new Font("SansSerif", Font.PLAIN, 16));
		if (achClear100) drawCenteredString(g, "✅ 100발 안에 클리어 달성!", 800, y + 25);
		else             drawCenteredString(g, "진행도: 현재 발사 수 " + shotsFiredRun + " / 100 (클리어 시 달성 판정)", 800, y + 25);

		y += 70;
		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		drawCenteredString(g, "• 1분 안에 클리어", 800, y);
		g.setFont(new Font("SansSerif", Font.PLAIN, 16));
		if (achClear1Min) {
			drawCenteredString(g, "✅ 1분 안에 클리어 달성!", 800, y + 25);
		} else {
			String recordText = (lastRunElapsedMs >= 0)
					? "최근 기록: " + formatMillis(lastRunElapsedMs) + " / 01:00 (클리어 시 달성 판정)"
					: "진행도: 01:00 이내 클리어 (클리어 시 달성 판정)";
			drawCenteredString(g, recordText, 800, y + 25);
		}

		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		drawCenteredString(g, "버튼을 눌러 메인으로 돌아갈 수 있습니다", 800, 420);
	}

	/* Back 버튼 좌표 고정 */
	private void layoutBackButton() {
		backButton.setBounds(20, 20, 140, 40);
	}

	/* ===== Utils ===== */
	private void drawToast(Graphics2D g) {
		if (toastText == null) return;
		long now = SystemTimer.getTime();
		if (now > toastUntil) return;

		int w = 480, h = 40;
		int x = (800 - w) / 2, y = 40;

		g.setColor(new Color(0,0,0,180));
		g.fillRoundRect(x, y, w, h, 16, 16);
		g.setColor(new Color(255,255,255,220));
		g.drawRoundRect(x, y, w, h, 16, 16);

		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		FontMetrics fm = g.getFontMetrics();
		int tx = x + (w - fm.stringWidth(toastText)) / 2;
		int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
		g.drawString(toastText, tx, ty);
	}

	private void showToast(String text, long durationMs) {
		this.toastText = text;
		this.toastUntil = SystemTimer.getTime() + durationMs;
	}

	private void drawCenteredString(Graphics2D g, String text, int width, int y) {
		FontMetrics fm = g.getFontMetrics();
		int x = (width - fm.stringWidth(text)) / 2;
		g.drawString(text, x, y);
	}

	private String formatMillis(long ms) {
		if (ms < 0) return "--:--";
		long totalSec = ms / 1000;
		long mm = totalSec / 60;
		long ss = totalSec % 60;
		return String.format("%02d:%02d", mm, ss);
	}

	/* ===== Input ===== */
	private class KeyInputHandler extends KeyAdapter {
		@Override public void keyPressed(KeyEvent e) {
			if (waitingForKeyPress) return;
			if (e.getKeyCode() == KeyEvent.VK_LEFT)  leftPressed  = true;
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
			if (e.getKeyCode() == KeyEvent.VK_SPACE) firePressed  = true;
		}
		@Override public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_LEFT)  leftPressed  = false;
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
			if (e.getKeyCode() == KeyEvent.VK_SPACE) firePressed  = false;
		}
		@Override public void keyTyped(KeyEvent e) { /* ignore */ }
	}

	/* ===== Button ===== */
	private static class MenuButton {
		private String label;
		private int x, y, w, h;
		MenuButton(String label) { this.label = label; }
		void setBounds(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
		boolean contains(int mx, int my) { return (mx >= x && mx <= x + w && my >= y && my <= y + h); }
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

	public static void main(String[] args) {
		Game g = new Game();
		g.gameLoop();
	}
}











