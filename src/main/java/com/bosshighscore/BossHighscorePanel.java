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
import java.util.List;
import java.util.stream.Stream;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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

	private MaterialTabGroup tabGroup;
	private MaterialTab friendsTab;
	private MaterialTab historyTab;

	private final PanelActions actions;

	private PanelState state = PanelState.EMPTY;
	private String selectedName;
	private String expandedName;

	BossHighscorePanel(PanelActions actions)
	{
		this.actions = actions;
		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(createHeader(), BorderLayout.NORTH);
		add(createTabs(), BorderLayout.CENTER);
		rebuild();
	}

	void render(PanelState state)
	{
		this.state = state;
		rebuild();
	}

	/** Shows the player in the tab they belong to: Friends for friends, History for everyone else. */
	void select(String name, boolean friend)
	{
		selectedName = name;
		expandedName = name;
		tabGroup.select(friend ? friendsTab : historyTab);
		rebuild();
	}

	/** For a searched player without boss kills: shown on the card, not added to a list. */
	void showWithoutKills(String name)
	{
		selectedName = name;
		expandedName = null;
		rebuild();
	}

	void setStatus(String text)
	{
		statusLabel.setText(text.isEmpty() ? " " : text);
	}

	private JPanel createHeader()
	{
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchField.addActionListener(e ->
		{
			actions.search(searchField.getText());
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

	private JPanel createTabs()
	{
		JButton refreshButton = new JButton("Refresh friends");
		refreshButton.addActionListener(e -> actions.refreshFriends());

		JPanel friendsContent = new JPanel(new BorderLayout(0, 6));
		friendsContent.setOpaque(false);
		friendsContent.add(friendsList, BorderLayout.CENTER);
		friendsContent.add(refreshButton, BorderLayout.SOUTH);

		JButton clearButton = new JButton("Clear history");
		clearButton.addActionListener(e -> confirmClearHistory());

		JPanel historyContent = new JPanel(new BorderLayout(0, 6));
		historyContent.setOpaque(false);
		historyContent.add(historyList, BorderLayout.CENTER);
		historyContent.add(clearButton, BorderLayout.SOUTH);

		JPanel display = new JPanel(new BorderLayout());
		display.setOpaque(false);

		tabGroup = new MaterialTabGroup(display);
		friendsTab = new MaterialTab("Friends", tabGroup, friendsContent);
		historyTab = new MaterialTab("History", tabGroup, historyContent);
		tabGroup.addTab(friendsTab);
		tabGroup.addTab(historyTab);
		tabGroup.select(friendsTab);

		JPanel tabs = new JPanel(new BorderLayout(0, 6));
		tabs.setOpaque(false);
		tabs.add(tabGroup, BorderLayout.NORTH);
		tabs.add(display, BorderLayout.CENTER);
		return tabs;
	}

	private void rebuild()
	{
		fillList(friendsList, state.getFriends(), false, "Log in to load your friends list");
		fillList(historyList, state.getHistory(), true, "Search a player to start");
		showSelected();
		revalidate();
		repaint();
	}

	private void showSelected()
	{
		PlayerTotals selected = Stream.concat(state.getFriends().stream(), state.getHistory().stream())
			.filter(player -> player.getName().equalsIgnoreCase(selectedName))
			.findFirst()
			.orElse(null);

		if (selected != null)
		{
			selectedNameLabel.setText(selected.getName());
			selectedTotalLabel.setText(formatKills(selected.totalKills(state.getCategories())));
		}
		else
		{
			// Searched players without boss kills aren't in either list, but the card should still match the search
			selectedNameLabel.setText(selectedName == null ? "Search a player" : selectedName);
			selectedTotalLabel.setText(selectedName == null ? "" : "0");
		}
	}

	private void fillList(JPanel list, List<PlayerTotals> players, boolean removable, String emptyHint)
	{
		list.removeAll();
		if (players.isEmpty())
		{
			list.add(smallLabel(emptyHint, ColorScheme.LIGHT_GRAY_COLOR));
			return;
		}

		for (int i = 0; i < players.size(); i++)
		{
			list.add(createRow(String.valueOf(i + 1), players.get(i), removable));
		}
	}

	private JPanel createRow(String rankText, PlayerTotals player, boolean removable)
	{
		JLabel rankLabel = smallLabel(rankText, ColorScheme.LIGHT_GRAY_COLOR);
		rankLabel.setPreferredSize(new Dimension(16, 0));

		JLabel nameLabel = new JLabel(player.getName());
		nameLabel.setForeground(Color.WHITE);

		JPanel nameAndUpdated = new JPanel(new GridLayout(2, 1));
		nameAndUpdated.setOpaque(false);
		nameAndUpdated.add(nameLabel);
		nameAndUpdated.add(smallLabel(formatUpdated(player.getUpdatedMillis()), ColorScheme.LIGHT_GRAY_COLOR));

		JLabel totalLabel = new JLabel(formatKills(player.totalKills(state.getCategories())));
		totalLabel.setForeground(ColorScheme.BRAND_ORANGE);

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(4, 6, 4, 6));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setToolTipText("Left-click for top bosses, right-click to refresh");
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
				else if (SwingUtilities.isRightMouseButton(e))
				{
					rowMenu(player.getName(), removable).show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.add(header, BorderLayout.NORTH);
		if (player.getName().equalsIgnoreCase(expandedName))
		{
			row.add(createDetails(player), BorderLayout.CENTER);
		}
		return row;
	}

	private JPanel createDetails(PlayerTotals player)
	{
		JPanel details = new JPanel(new BorderLayout(0, 6));
		details.setOpaque(false);
		details.setBorder(new EmptyBorder(0, 28, 6, 6));
		details.add(createBossList(player), BorderLayout.CENTER);

		PlayerGains gains = state.getGains().get(PlayerStore.key(player.getName()));
		if (gains != null)
		{
			details.add(createGains(gains), BorderLayout.SOUTH);
		}
		return details;
	}

	private JPanel createBossList(PlayerTotals player)
	{
		JPanel list = new JPanel(new GridLayout(0, 1, 0, 2));
		list.setOpaque(false);

		List<BossKills> topBosses = player.topBosses(state.getTopBossCount(), state.getCategories());
		for (int i = 0; i < topBosses.size(); i++)
		{
			BossKills boss = topBosses.get(i);
			JPanel line = new JPanel(new BorderLayout(6, 0));
			line.setOpaque(false);
			line.add(smallLabel((i + 1) + ". " + BossNames.shortName(boss.getBoss()), ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.CENTER);
			line.add(smallLabel(formatKills(boss.getKills()), Color.WHITE), BorderLayout.EAST);
			list.add(line);
		}
		return list;
	}

	private JPanel createGains(PlayerGains gains)
	{
		JPanel grid = new JPanel(new GridLayout(2, GainPeriod.values().length, 4, 0));
		grid.setOpaque(false);
		for (GainPeriod period : GainPeriod.values())
		{
			grid.add(smallLabel(period.getLabel(), ColorScheme.LIGHT_GRAY_COLOR));
		}

		LocalDate today = LocalDate.now();
		for (GainPeriod period : GainPeriod.values())
		{
			int gain = gains.gain(period, state.getCategories(), today, ZoneId.systemDefault());
			Color color = gain > 0 ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR;
			grid.add(smallLabel("+" + formatKills(gain), color));
		}
		return grid;
	}

	private void confirmClearHistory()
	{
		int choice = JOptionPane.showConfirmDialog(this,
			"Remove every player from your search history?", "Clear history",
			JOptionPane.YES_NO_OPTION);
		if (choice == JOptionPane.YES_OPTION)
		{
			actions.clearHistory();
		}
	}

	private JPopupMenu rowMenu(String name, boolean removable)
	{
		JPopupMenu menu = new JPopupMenu();
		JMenuItem refresh = new JMenuItem("Refresh");
		refresh.addActionListener(e -> actions.refreshPlayer(name));
		menu.add(refresh);

		if (removable)
		{
			JMenuItem remove = new JMenuItem("Remove from history");
			remove.addActionListener(e -> actions.removeFromHistory(name));
			menu.add(remove);
		}
		return menu;
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

	private static String formatKills(int kills)
	{
		return String.format("%,d", kills);
	}

	static String formatUpdated(long millis)
	{
		ZonedDateTime updated = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
		boolean today = updated.toLocalDate().equals(LocalDate.now());
		return "updated " + (today ? TIME : DATE).format(updated);
	}
}
