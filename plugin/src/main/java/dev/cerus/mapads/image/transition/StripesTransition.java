package dev.cerus.mapads.image.transition;

import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.scheduler.ExecutorServiceScheduler;
import dev.cerus.mapads.scheduler.Scheduler;
import dev.cerus.mapads.scheduler.SchedulerRunnable;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.api.graphics.MapGraphics;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StripesTransition implements Transition {

    private final Scheduler scheduler = ExecutorServiceScheduler.create(Executors.newScheduledThreadPool(1));
    private final int size;

    public StripesTransition(final int size) {
        this.size = size;
    }

    @Override
    public void makeTransition(@NotNull final MapScreen screen, @Nullable final MapImage oldImg, @NotNull final MapImage newImg) {
        if (oldImg != null && oldImg.getId().equals(newImg.getId())) {
            return;
        }

        final MapGraphics<?, ?> graphics = screen.getGraphics();
        this.scheduler.scheduleAtFixedRate(new SchedulerRunnable() {
            private int col = 0;
            private int row = 0;
            private boolean dir = false; // True: Up, False: Down

            @Override
            public void run() {
                if (this.row > (screen.getHeight() * 128) / StripesTransition.this.size) {
                    this.row = 0;
                    this.col++;
                    this.dir = !this.dir;
                }
                if (this.col > (screen.getWidth() * 128) / StripesTransition.this.size) {
                    this.cancel();
                    screen.sendMaps(true, ReviewerUtil.getNonReviewingPlayers(screen));
                    return;
                }

                final int fromX = this.col * StripesTransition.this.size;
                final int toX = this.col * StripesTransition.this.size + StripesTransition.this.size;
                final int fromY = (this.dir ? (screen.getHeight() * 128) - (this.row * StripesTransition.this.size) - StripesTransition.this.size : this.row * StripesTransition.this.size);
                final int toY = (this.dir ? (screen.getHeight() * 128) - (this.row * StripesTransition.this.size) : this.row * StripesTransition.this.size + StripesTransition.this.size);

                //Bukkit.broadcastMessage(this.col + "," + this.row + " / " + ((screen.getWidth() * 128) / STEP) + "," + ((screen.getHeight() * 128) / STEP));
                //Bukkit.broadcastMessage(fromX + "->" + toX + ", " + fromY + "->" + toY);

                if (graphics.hasDirectAccessCapabilities() && false) {
                    final MapGraphics<?, ?> imgGraphics = newImg.getGraphics();
                    for (int y = fromY; y < toY; y++) {
                        System.arraycopy(imgGraphics.getDirectAccessData(),
                                imgGraphics.index(fromX, y),
                                graphics.getDirectAccessData(),
                                fromX,
                                toX - fromX);
                    }
                } else {
                    for (int x = fromX; x < toX; x++) {
                        for (int y = fromY; y < toY; y++) {
                            graphics.setPixel(x, y, newImg.getGraphics().getPixel(x, y));
                        }
                    }
                }

                this.row++;
                screen.sendMaps(false, ReviewerUtil.getNonReviewingPlayers(screen));
            }
        }, 0, 1000 / 20, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cleanup() throws Exception {
        this.scheduler.close();
    }

}
