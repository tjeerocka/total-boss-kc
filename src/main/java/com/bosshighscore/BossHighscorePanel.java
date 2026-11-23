package com.bosshighscore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/** Search box, selected player card and the Friends / History lists. Only touch it on the Swing thread. */
class BossHighscorePanel extends PluginPanel
{
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM");

	private final IconTextField searchField = new IconTextField();
	private final JLabel selectedNameLabel = new JLabel();
	private final JLabel selectedTotalLabel = new JLabel();
	private final JLabel statusLabel = new JLabel(" ");
	private final JPanel friendsList = createListPanel();
	private final JPanel historyList = createListPanel();

	private List<PlayerTotals> friends = Collections.emptyList();
	private List<PlayerTotals> history = Collections.emptyList();
	private int topBossCount;
	private String selectedName;
	private String expandedName;

	BossHighscorePanel(Consumer<String> onSearch, Runnable onRefreshFriends)
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(createHeader(onSearch), BorderLayout.NORTH);
		add(createTabs(onRefreshFriends), BorderLayout.CENTER);
		rebuild();
	}

	void render(List<PlayerTotals> friends, List<PlayerTotals> history, int topBossCount)
	{
		this.friends = friends;
		this.history = history;
		this.topBossCount = topBossCount;
		rebuild();
	}

	void select(String name)
	{
		selectedName = name;
		expandedName = name;
		rebuild();
	}

	void setStatus(String text)
	{
		statusLabel.setText(text.isEmpty() ? " " : text);
	}

	private JPanel createHeader(Consumer<String> onSearch)
	{
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchField.addActionListener(e ->
		{
			onSearch.accept(searchField.getText());
			searchField.setText("");
		});

		selectedNameLabel.setForeground(Color.WHITE);
		selectedTotalLabel.setForeground(ColorScheme.BRAND_ORANGE);
		selectedTotalLabel.setFont(FontManager.getRunescapeBoldFont());

		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(8, 8, 8, 8));
		card.add(selectedNameLabel, BorderLayout.WEST);
		card.add(selectedTotalLabel, BorderLayout.EAST);

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout(0, 6));
		header.setOpaque(false);
		header.add(searchField, BorderLayout.NORTH);
		header.add(card, BorderLayout.CENTER);
		header.add(statusLabel, BorderLayout.SOUTH);
		return header;
	}

	private JPanel createTabs(Runnable onRefreshFriends)
	{
		JButton refreshButton = new JButton("Refresh friends");
		refreshButton.addActionListener(e -> onRefreshFriends.run());

		JPanel friendsTab = new JPanel(new BorderLayout(0, 6));
		friendsTab.setOpaque(false);
		friendsTab.add(friendsList, BorderLayout.CENTER);
		friendsTab.add(refreshButton, BorderLayout.SOUTH);

		JPanel display = new JPanel(new BorderLayout());
		display.setOpaque(false);

		MaterialTabGroup tabGroup = new MaterialTabGroup(display);
		MaterialTab friends = new MaterialTab("Friends", tabGroup, friendsTab);
		MaterialTab history = new MaterialTab("History", tabGroup, historyList);
		tabGroup.addTab(friends);
		tabGroup.addTab(history);
		tabGroup.select(friends);

		JPanel tabs = new JPanel(new BorderLayout(0, 6));
		tabs.setOpaque(false);
		tabs.add(tabGroup, BorderLayout.NORTH);
		tabs.add(display, BorderLayout.CENTER);
		return tabs;
	}

	private void rebuild()
	{
		fillList(friendsList, friends, true, "Log in to load your friends list");
		fillList(historyList, history, false, "Search a player to start");
		showSelected();
		revalidate();
		repaint();
	}

	private void showSelected()
	{
		PlayerTotals selected = Stream.concat(friends.stream(), history.stream())
			.filter(player -> player.getName().equalsIgnoreCase(selectedName))
			.findFirst()
			.orElse(null);

		selectedNameLabel.setText(selected == null ? "Search a player" : selected.getName());
		selectedTotalLabel.setText(selected == null ? "" : formatTotal(selected));
	}

	private void fillList(JPanel list, List<PlayerTotals> players, boolean ranked, String emptyHint)
	{
		list.removeAll();
		if (players.isEmpty())
		{
			list.add(smallLabel(emptyHint, ColorScheme.LIGHT_GRAY_COLOR));
			return;
		}

		int rank = 1;
		for (PlayerTotals player : players)
		{
			String rankText = ranked && player.isRanked() ? String.valueOf(rank++) : "";
			list.add(createRow(rankText, player));
		}
	}

	private JPanel createRow(String rankText, PlayerTotals player)
	{
		JLabel rankLabel = smallLabel(rankText, ColorScheme.LIGHT_GRAY_COLOR);
		rankLabel.setPreferredSize(new Dimension(16, 0));

		JLabel nameLabel = new JLabel(player.getName());
		nameLabel.setForeground(Color.WHITE);

		JPanel nameAndUpdated = new JPanel(new GridLayout(2, 1));
		nameAndUpdated.setOpaque(false);
		nameAndUpdated.add(nameLabel);
		nameAndUpdated.add(smallLabel(formatUpdated(player.getUpdatedMillis()), ColorScheme.LIGHT_GRAY_COLOR));

		JLabel totalLabel = new JLabel(formatTotal(player));
		totalLabel.setForeground(player.isRanked() ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(4, 6, 4, 6));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setToolTipText("Click to show top bosses");
		header.add(rankLabel, BorderLayout.WEST);
		header.add(nameAndUpdated, BorderLayout.CENTER);
		header.add(totalLabel, BorderLayout.EAST);
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					toggle(player.getName());
				}
			}
		});

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.add(header, BorderLayout.NORTH);
		if (player.getName().equalsIgnoreCase(expandedName))
		{
			row.add(createBossList(player), BorderLayout.CENTER);
		}
		return row;
	}

	private JPanel createBossList(PlayerTotals player)
	{
		JPanel list = new JPanel(new GridLayout(0, 1, 0, 2));
		list.setOpaque(false);
		list.setBorder(new EmptyBorder(0, 28, 6, 6));

		List<BossKills> topBosses = player.topBosses(topBossCount);
		if (topBosses.isEmpty())
		{
			list.add(smallLabel("No boss above 5 kills", ColorScheme.LIGHT_GRAY_COLOR));
		}
		for (int i = 0; i < topBosses.size(); i++)
		{
			BossKills boss = topBosses.get(i);
			JPanel line = new JPanel(new BorderLayout(6, 0));
			line.setOpaque(false);
			line.add(smallLabel((i + 1) + ". " + boss.getBoss(), ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.CENTER);
			line.add(smallLabel(String.format("%,d", boss.getKills()), Color.WHITE), BorderLayout.EAST);
			list.add(line);
		}
		return list;
	}

	private void toggle(String name)
	{
		boolean wasExpanded = name.equalsIgnoreCase(expandedName);
		expandedName = wasExpanded ? null : name;
		selectedName = name;
		rebuild();
	}

	private static JPanel createListPanel()
	{
		JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
		list.setOpaque(false);
		return list;
	}

	private static JLabel smallLabel(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	private static String formatTotal(PlayerTotals player)
	{
		return player.isRanked() ? String.format("%,d", player.getTotalKills()) : "unranked";
	}

	static String formatUpdated(long millis)
	{
		ZonedDateTime updated = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
		boolean today = updated.toLocalDate().equals(LocalDate.now());
		return "updated " + (today ? TIME : DATE).format(updated);
	}
}
