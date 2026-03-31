package com.awakened;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Random;

@Singleton
@Slf4j
public class FakeHead
{
    Client client;
    public static Prayer prayer;


    @Inject
    public FakeHead(Client client)
    {
        this.client = client;
    }

    public void onGameTick(GameTick gameTick) {
        for (NPC npc : client.getNpcs())
        {
            if (npc.getId() != net.runelite.api.gameval.NpcID.VARDORVIS) continue;
            System.out.println(npc.getAnimationFrame());
            if ((npc.getAnimation() == 10340 && npc.getAnimationFrame() < 10))
            {
                RuneLiteObject runeliteObject = client.createRuneLiteObject();
                runeliteObject.setModel(client.loadModel(49301));
                // TODO: Animation
                runeliteObject.setAnimation(client.loadAnimation(10348));

                var center = LocalPoint.fromWorld(client.getTopLevelWorldView(), WorldPoint.toLocalInstance(client.getTopLevelWorldView(), new WorldPoint(1129, 3419, 0)).iterator().next());
                runeliteObject.setLocation(center, 0);
                setOrientationGoalAsPlayer(runeliteObject);
                runeliteObject.setActive(true);
                runeliteObject.getAnimationController().setOnFinished(animationController -> {
                    runeliteObject.setActive(false);
                });
                // int id, int plane, int startX, int startY, int startZ, int startCycle, int endCycle,
                //		int slope, int startHeight, int endHeight, @Nullable Actor target, int targetX, int targetY
                int tileHeight = client.getTileHeights()[0]
                        [client.getLocalPlayer().getLocalLocation().getSceneX()]
                        [client.getLocalPlayer().getLocalLocation().getSceneY()];


                // 2520 // Mage
                int projectileID = randomBetween(2520, 2521);
                client.playSoundEffect(7124);
                Projectile proj = client.createProjectile(projectileID,
                        client.getPlane(),
                        center.getX(),
                        center.getY(),
                        150, // z coordinate
                        client.getGameCycle(),  // start cycle
                        client.getGameCycle() + 30,  // end cycle
                        50, // slope ???
                        30, // start height
                        150, // end height
                        client.getLocalPlayer(),
                        client.getLocalPlayer().getLocalLocation().getX(),
                        client.getLocalPlayer().getLocalLocation().getY()
                );
                client.getProjectiles()
                        .addLast(proj);
                prayer = projectileID == 2520 ? Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MISSILES;
                // Place model somewhere in 5x5 area, within the region
            }
        }
    }

    private static int randomBetween(int min, int max)
    {
        return new Random().nextInt(max - min + 1) + min;
    }

    public static int getDamage(Client client)
    {
        if (prayer == null) return 0;
        int damageTaken = client.isPrayerActive(prayer) ? 0: 1;
        prayer = null;
        return damageTaken;
    }


    public int calculateRotationFromAtoB(double aX, double aY, double bX, double bY)
    {
        double xDiff = bX - aX;
        double yDiff = bY - aY;

        double angle = 0;

        double aToBAngle = Math.abs(Math.atan(yDiff / xDiff));

        if (xDiff > 0 && yDiff <= 0)
        {
            angle = aToBAngle + (3 * Math.PI / 2);
        }
        else if (xDiff >= 0 && yDiff > 0)
        {
            angle = Math.atan(xDiff / yDiff) + Math.PI;
        }
        else if (xDiff < 0 && yDiff >= 0)
        {
            angle = aToBAngle + (Math.PI / 2);
        }
        else if (xDiff <= 0 && yDiff < 0)
        {
            angle = Math.atan(xDiff / yDiff);
        }

        int nextOrientation = (int) ((angle * 1024 / Math.PI) - 1024);
        if (nextOrientation < 0)
        {
            nextOrientation += 2048;
        }

        return nextOrientation;
    }

    public void setOrientationGoalAsPlayer(RuneLiteObject runeLiteObject)
    {
        double playerX = client.getLocalPlayer().getLocalLocation().getX();
        double playerY = client.getLocalPlayer().getLocalLocation().getY();
        double currentNPCX = runeLiteObject.getLocation().getX();
        double currentNPCY = runeLiteObject.getLocation().getY();

        int newOrientation = calculateRotationFromAtoB(playerX, playerY, currentNPCX, currentNPCY);
        runeLiteObject.setOrientation(newOrientation);
    }
}
