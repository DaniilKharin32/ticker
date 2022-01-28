package com.robinhood.ticker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.anyChar;
import static org.mockito.Mockito.when;

public class TickerColumnManagerTest {
    @Mock
    TickerDrawMetrics metrics;

    private TickerColumnManager tickerColumnManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(metrics.getCharWidth(anyString())).thenReturn(5f);
        when(metrics.getCharWidth(TickerUtils.EMPTY_CHAR)).thenReturn(0f);
        when(metrics.getPreferredScrollingDirection()).thenReturn(TickerView.ScrollingDirection.ANY);

        tickerColumnManager = new TickerColumnManager(metrics);
        tickerColumnManager.setCharacterLists("1234567890");
    }

    @Test
    public void test_setText_animate() {
        assertEquals(0, numberOfTickerColumns());

        tickerColumnManager.setText("1234");
        tickerColumnManager.setAnimationProgress(1f);
        assertEquals(4, numberOfTickerColumns());
        assertEquals("1", String.valueOf(tickerColumnAtIndex(0).getTargetChar()));
        assertEquals("2", String.valueOf(tickerColumnAtIndex(1).getTargetChar()));
        assertEquals("3", String.valueOf(tickerColumnAtIndex(2).getTargetChar()));
        assertEquals("4", String.valueOf(tickerColumnAtIndex(3).getTargetChar()));

        tickerColumnManager.setText("999");
        assertEquals(4, numberOfTickerColumns());
        assertEquals(TickerUtils.EMPTY_CHAR, tickerColumnAtIndex(0).getTargetChar());
        assertEquals("9", String.valueOf(tickerColumnAtIndex(1).getTargetChar()));
        assertEquals("9", String.valueOf(tickerColumnAtIndex(2).getTargetChar()));
        assertEquals("9", String.valueOf(tickerColumnAtIndex(3).getTargetChar()));

        tickerColumnManager.setAnimationProgress(1f);
        tickerColumnManager.setText("899");
        assertEquals(3, numberOfTickerColumns());
        assertEquals("8", String.valueOf(tickerColumnAtIndex(0).getTargetChar()));
        assertEquals("9", String.valueOf(tickerColumnAtIndex(1).getTargetChar()));
        assertEquals("9", String.valueOf(tickerColumnAtIndex(2).getTargetChar()));
    }

    @Test
    public void test_setText_noAnimate() {
        assertEquals(0, numberOfTickerColumns());

        tickerColumnManager.setText("1234");
        assertEquals(4, numberOfTickerColumns());
        assertEquals("1", String.valueOf(tickerColumnAtIndex(0).getTargetChar()));
        assertEquals("2", String.valueOf(tickerColumnAtIndex(1).getTargetChar()));
        assertEquals("3", String.valueOf(tickerColumnAtIndex(2).getTargetChar()));
        assertEquals("4", String.valueOf(tickerColumnAtIndex(3).getTargetChar()));

        tickerColumnManager.setText("999");
        assertEquals(3, numberOfTickerColumns());
        assertEquals("9", String.valueOf(tickerColumnAtIndex(0).getTargetChar()));
        assertEquals("9", String.valueOf(tickerColumnAtIndex(1).getTargetChar()));
        assertEquals("9", String.valueOf(tickerColumnAtIndex(2).getTargetChar()));
    }

    private TickerColumn tickerColumnAtIndex(int index) {
        return tickerColumnManager.tickerColumns.get(index);
    }

    private int numberOfTickerColumns() {
        return tickerColumnManager.tickerColumns.size();
    }
}
