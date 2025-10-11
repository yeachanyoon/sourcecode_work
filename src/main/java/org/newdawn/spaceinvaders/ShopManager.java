package org.newdawn.spaceinvaders;

import javax.swing.*;
import java.awt.Color; // Color 클래스를 사용하기 위해 import

public class ShopManager {
    private final Game game;

    public ShopManager(Game game) {
        this.game = game;
    }

    public void showShopDialog() {
        // --- 아이템 목록 확장 ---
        String[] options = {
                "Ship: Red (Default)",
                "Ship: Green (10 Tokens)",
                "Ship: Blue (15 Tokens)",
                "Ship: Gold (25 Tokens)",
                "BG: Blue (5 Tokens)",
                "BG: Gray (5 Tokens)",
                "Cancel"
        };

        String message = "Welcome to the Shop!\nYour Tokens: " + game.getTokens();

        int choice = JOptionPane.showOptionDialog(
                game.getContainer(),
                message,
                "Item Shop",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        // --- 선택지에 따라 다른 동작을 하도록 switch 문 확장 ---
        switch (choice) {
            case 0: // 기본 (빨간색) 함선
                game.setCurrentShipSprite("sprites/ship.gif");
                JOptionPane.showMessageDialog(game.getContainer(), "Red ship equipped.");
                break;
            case 1: // 녹색 함선
                purchaseItem(10, "sprites/ship_green.gif", "ship");
                break;
            case 2: // 파란색 함선
                purchaseItem(15, "sprites/ship_blue.gif", "ship");
                break;
            case 3: // 황금색 함선
                purchaseItem(25, "sprites/ship_gold.gif", "ship");
                break;
            case 4: // 파란 배경
                // Color 객체를 직접 전달하도록 수정
                purchaseItem(5, Color.BLUE, "background");
                break;
            case 5: // 회색 배경
                // Color 객체를 직접 전달하도록 수정
                purchaseItem(5, Color.DARK_GRAY, "background");
                break;
            default: // 취소 또는 창 닫기
                break;
        }
    }

    // 아이템 타입을 구분하기 위해 메소드 오버로딩 (Overloading) 사용
    private void purchaseItem(int cost, String spritePath, String itemType) {
        if ("ship".equals(itemType)) {
            if (game.getTokens() >= cost) {
                game.spendTokens(cost);
                game.setCurrentShipSprite(spritePath);
                JOptionPane.showMessageDialog(game.getContainer(), "Purchase successful! New ship equipped.");
            } else {
                JOptionPane.showMessageDialog(game.getContainer(), "Not enough tokens!");
            }
        }
    }

    private void purchaseItem(int cost, Color color, String itemType) {
        if ("background".equals(itemType)) {
            if (game.getTokens() >= cost) {
                game.spendTokens(cost);
                game.setBackground(color); // Game 클래스의 setBackground 메소드도 Color 객체를 받도록 수정 필요
                JOptionPane.showMessageDialog(game.getContainer(), "Purchase successful! New background applied.");
            } else {
                JOptionPane.showMessageDialog(game.getContainer(), "Not enough tokens!");
            }
        }
    }
}
