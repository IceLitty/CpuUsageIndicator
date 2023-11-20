package krasa.cpu;

/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.intellij.ide.DataManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.JreHiDpiUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class CpuUsagePanel extends TextPanel implements CustomStatusBarWidget {
	@NonNls
	public static final String WIDGET_ID = "krasa.cpu.CpuUsagePanel";
	private static final String SAMPLE_STRING = " 100% / 100% ";

	private final Color ideColor = JBColor.namedColor("MemoryIndicator.usedBackground", new JBColor(Gray._185, Gray._110));
	private final Color systemColor = JBColor.namedColor("MemoryIndicator.allocatedBackground", new JBColor(Gray._215, Gray._90));

	private Project myProject;
	private final String projectName;

	private volatile int myLastSystem = -1;
	private volatile int myLastProcess = -1;
	private volatile Image myBufferedImage;
	private volatile boolean myWasPressed;

	public CpuUsagePanel(Project project) {
		this.myProject = project;
		this.projectName = project.getName();

		setOpaque(false);
		setFocusable(false);
		setToolTipText("IDE CPU usage / System CPU usage");


		setBorder(BorderFactory.createEmptyBorder());
		updateUI();

		new UiNotifyConnector(this, new Activatable() {

			@Override
			public void showNotify() {
				CpuUsageManager.register(CpuUsagePanel.this);
			}

			@Override
			public void hideNotify() {
				CpuUsageManager.unregister(CpuUsagePanel.this);
			}
		});
		MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
//				if (SwingUtilities.isLeftMouseButton(e)) {
//					CpuUsageManager.update();
//					final DataContext context = DataManager.getInstance().getDataContext(CpuUsagePanel.this);
//					ActionManager.getInstance().getAction("TakeThreadDump").actionPerformed(new AnActionEvent(e, context, ActionPlaces.UNKNOWN, new Presentation(""), ActionManager.getInstance(), 0));
//				} else if (SwingUtilities.isRightMouseButton(e)) {
				final DataContext context = DataManager.getInstance().getDataContext(CpuUsagePanel.this);
				ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, getActionGroup(), context, JBPopupFactory.ActionSelectionAid.MNEMONICS, false);

				Dimension dimension = popup.getContent().getPreferredSize();
				Point at = new Point(0, -dimension.height);
				popup.show(new RelativePoint(e.getComponent(), at));
//				}
			}
		};
		addMouseListener(mouseAdapter);
	}

	@NotNull
	private DefaultActionGroup getActionGroup() {
		DumbAwareAction dumbAwareAction1 = (DumbAwareAction) ActionManager.getInstance().getAction("TakeThreadDump");
		DumbAwareAction dumbAwareAction2 = (DumbAwareAction) ActionManager.getInstance().getAction("OpenLastUiFreezeThreadDump");
		DumbAwareAction dumbAwareAction3 = (DumbAwareAction) ActionManager.getInstance().getAction("OpenPerformanceWatcherSettings");
		return new DefaultActionGroup(dumbAwareAction1, dumbAwareAction2, dumbAwareAction3);
	}

	@Override
	public void dispose() {
		myProject = null;
		CpuUsageManager.unregister(CpuUsagePanel.this);
	}

	@Override
	public void install(@NotNull StatusBar statusBar) {
	}


	@Override
	@NotNull
	public String ID() {
		return WIDGET_ID;
	}

	public void setShowing(final boolean showing) {
		if (showing != isVisible()) {
			setVisible(showing);
			revalidate();
		}
	}

	@Override
	public void updateUI() {
		myBufferedImage = null;
		super.updateUI();
		setFont(getWidgetFont());
		setBorder(BorderFactory.createEmptyBorder());
	}

	private static Font getWidgetFont() {
		return JBUI.Fonts.label();
	}

	@Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public void paintComponent(@NotNull final Graphics g) {
		Image bufferedImage;

		final Dimension size = getSize();
		//rare error
		if (size.width <= 0 || size.height <= 0) {
			return;
		}

		final Insets insets = getInsets();

		bufferedImage = ImageUtil.createImage(g, size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = (Graphics2D) bufferedImage.getGraphics().create();

		final int max = 100;
		int system = CpuUsageManager.system;
		int process = CpuUsageManager.process;
		final int otherProcesses = Math.max(system - process, 0);

		final int totalBarLength = size.width - insets.left - insets.right;
		final int processUsageBarLength = totalBarLength * process / max;
		final int otherProcessesUsageBarLength = totalBarLength * otherProcesses / max;
		final int barHeight = size.height;
		final int yOffset = 0;
		final int xOffset = insets.left;

		// background
		g2.setColor(UIUtil.getPanelBackground());
		g2.fillRect(0, 0, size.width, size.height);

		// gauge (ide)
		g2.setColor(ideColor);
		g2.fillRect(0, 0, processUsageBarLength + 1, barHeight);

		// gauge (system)
		g2.setColor(systemColor);
		g2.fillRect(processUsageBarLength + 1, 0, otherProcessesUsageBarLength + 1, barHeight);

		// label
		g2.setFont(getFont());
		// final String info = CpuUsageBundle.message("cpu.usage.panel.message.text", CpuUsageManager.process, CpuUsageManager.system);
		final String info = fixedLengthString(String.valueOf(process), 3) + "% / " + fixedLengthString(String.valueOf(system), 3) + "%";

		final FontMetrics fontMetrics = g.getFontMetrics();
		final int infoWidth = fontMetrics.charsWidth(info.toCharArray(), 0, info.length());
		final int infoHeight = fontMetrics.getAscent();
		UISettings.setupAntialiasing(g2);

		final Color fg = JBColor.foreground();
		g2.setColor(fg);
		g2.drawString(info, xOffset + (totalBarLength - infoWidth) / 2, yOffset + infoHeight + (barHeight - infoHeight) / 2 - 1);

		g2.dispose();
		myBufferedImage = bufferedImage;

		draw(g, bufferedImage);
	}

	/**
	 * it will probably be better synchronized, not sure
	 */
	private synchronized void draw(Graphics g, Image bufferedImage) {
		UIUtil.drawImage(g, bufferedImage, 0, 0, null);
		if (JreHiDpiUtil.isJreHiDPI((Graphics2D) g) && !UIUtil.isUnderDarcula()) {
			Graphics2D g2 = (Graphics2D) g.create(0, 0, getWidth(), getHeight());
			float s = JBUIScale.sysScale(g2);
			g2.scale(1 / s, 1 / s);
			g2.setColor(UIUtil.isUnderIntelliJLaF() ? Gray.xC9 : Gray.x91);
			g2.drawLine(0, 0, (int) (s * getWidth()), 0);
			g2.scale(1, 1);
			g2.dispose();
		}
	}

	public static String fixedLengthString(String string, int length) {
		return String.format("%1$" + length + "s", string);
	}

	@NotNull
	@Override
	public Dimension getPreferredSize() {
		final Insets insets = getInsets();
		int width = getFontMetrics(getWidgetFont()).stringWidth(SAMPLE_STRING) + insets.left + insets.right + JBUI.scale(2);
		int height = getFontMetrics(getWidgetFont()).getHeight() + insets.top + insets.bottom + JBUI.scale(2);
		return new Dimension(width, height);
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	public boolean update() {
		boolean painted = false;
		if (!isShowing()) {
			// noinspection ConstantConditions
			return painted;
		}

		if (CpuUsageManager.system != myLastSystem || CpuUsageManager.process != myLastProcess) {
			myLastSystem = CpuUsageManager.system;
			myLastProcess = CpuUsageManager.process;
			myBufferedImage = null;

			Graphics graphics = getGraphics();
			if (graphics != null) {
				paintComponent(graphics);
				painted = true;
			}

		}
		return painted;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("projectName", projectName).toString();
	}
}
