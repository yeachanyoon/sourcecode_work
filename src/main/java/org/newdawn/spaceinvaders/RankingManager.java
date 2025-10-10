package org.newdawn.spaceinvaders;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RankingManager {
    private static final String RANKING_FILE = "ranking.dat";
    private List<PlayerScore> scores;

    public RankingManager() {
        this.scores = loadScores();
    }

    @SuppressWarnings("unchecked")
    private ArrayList<PlayerScore> loadScores() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(RANKING_FILE))) {
            return (ArrayList<PlayerScore>) ois.readObject();
        } catch (FileNotFoundException e) {
            return new ArrayList<>(); // 파일이 없으면 새 리스트 반환
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RANKING_FILE))) {
            oos.writeObject(scores);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addScore(String name, int score) {
        scores.add(new PlayerScore(name, score));
        Collections.sort(scores); // 점수에 따라 정렬
        saveScores();
    }

    public void showRankingBoard(JFrame parent) {
        StringBuilder rankingText = new StringBuilder("<html><h1>Ranking</h1><table border='1'>");
        rankingText.append("<tr><th>Rank</th><th>Name</th><th>Score</th></tr>");

        int rank = 1;
        for (PlayerScore ps : scores) {
            if (rank > 10) break; // 상위 10개만 보여주기
            rankingText.append("<tr><td>").append(rank).append("</td>");
            rankingText.append("<td>").append(ps.getName()).append("</td>");
            rankingText.append("<td>").append(ps.getScore()).append("</td></tr>");
            rank++;
        }
        rankingText.append("</table></html>");

        JOptionPane.showMessageDialog(parent, rankingText.toString(), "Ranking Board", JOptionPane.INFORMATION_MESSAGE);
    }
}