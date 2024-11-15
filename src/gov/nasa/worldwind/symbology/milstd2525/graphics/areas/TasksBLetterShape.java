/* 
 * Copyright 2006-2009, 2017, 2020 United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 * 
 * The NASA World Wind Java (WWJ) platform is licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 * NASA World Wind Java (WWJ) also contains the following 3rd party Open Source
 * software:
 * 
 *     Jackson Parser – Licensed under Apache 2.0
 *     GDAL – Licensed under MIT
 *     JOGL – Licensed under  Berkeley Software Distribution (BSD)
 *     Gluegen – Licensed under Berkeley Software Distribution (BSD)
 * 
 * A complete listing of 3rd Party software notices and licenses included in
 * NASA World Wind Java (WWJ)  can be found in the WorldWindJava-v2.2 3rd-party
 * notices and licenses PDF found in code directory.
 */

package gov.nasa.worldwind.symbology.milstd2525.graphics.areas;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.symbology.TacticalGraphicLabel;
import gov.nasa.worldwind.symbology.milstd2525.AbstractMilStd2525TacticalGraphic;
import gov.nasa.worldwind.symbology.milstd2525.graphics.TacGrpSidc;
import gov.nasa.worldwind.util.Logging;

import java.util.*;

/**
 * Implementation of block graphics. This class implements the following graphics:
 * <p>
 * <ul> <li>Block (2.X.1.1)</li> <li>Penetrate (2.X.1.17)</li> </ul>
 *
 * @author maciejziolkowski
 * @version $Id: TaskBLetterShape.java 1 2017-10-04 17:45:02Z maciejziolkowski $
 */
public class TasksBLetterShape extends AbstractMilStd2525TacticalGraphic
{

    public TasksBLetterShape(String sidc)
    {
        super(sidc);
    }

    /**
     * Indicates the graphics supported by this class.
     *
     * @return SIDC string that identify graphics that this class supports.
     */
    public static List<String> getSupportedGraphics()
    {
        return Arrays.asList(TacGrpSidc.TSK_BLK,
            TacGrpSidc.TSK_PNE);
    }

    /**
     * Create the label on the graphic.
     */
    @Override
    protected void createLabels()
    {
        String code = this.maskedSymbolCode;
        if (TacGrpSidc.TSK_PNE.equals(code))
        {
            addLabel("P");
        }
        else
        {
            addLabel("B");
        }
    }

    /**
     * Determine the appropriate position for the graphic's labels.
     *
     * @param dc Current draw context.
     */
    @Override
    protected void determineLabelPositions(DrawContext dc)
    {
        Iterator<? extends Position> iterator = this.paths.get(1).getPositions().iterator();

        // Find the first and last positions on the path
        Position first = iterator.next();
        Position second = first;
        while (iterator.hasNext())
        {
            second = iterator.next();
        }

        TacticalGraphicLabel startLabel = this.labels.get(0);

        // Position the labels at the ends of the path
        LatLon ll = LatLon.interpolate(0.5, first, second);
        startLabel.setPosition(new Position(ll, 0));
        startLabel.setOrientationPosition(second);
        startLabel.setTextAlign(AVKey.LEFT);
    }

    /**
     * Center point beatween point1 and point2
     */
    private LatLon center;
    /**
     * Distance between Arrow position and point1
     */
    private Angle length;
    private double lengthDouble;
    /**
     * Distance between center and point3
     */
    private Angle distance;
    private double distanceDouble;
    /**
     * Angle
     */
    private Angle ang;
    private Angle ang0;
    private double angDouble;

    /**
     * First control point.
     */
    protected Position point1;
    /**
     * Second control point.
     */
    protected Position point2;
    /**
     * Third control point.
     */
    protected Position point3;

    /**
     * Path used to render the line.
     */
    protected ArrayList<Path> paths;

    /**
     * {@inheritDoc}
     *
     * @param positions Control points that orient the graphic. Must provide at least three points.
     */
    public void setPositions(Iterable<? extends Position> positions)
    {
        if (positions == null)
        {
            String message = Logging.getMessage("nullValue.PositionsListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            Iterator<? extends Position> iterator = positions.iterator();
            this.point1 = iterator.next();
            this.point2 = iterator.next();
            this.point3 = iterator.next();
        }
        catch (NoSuchElementException e)
        {
            String message = Logging.getMessage("generic.InsufficientPositions");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.paths = null; // Need to recompute path for the new control points
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<? extends Position> getPositions()
    {
        return Arrays.asList(this.point1, this.point2, this.point3);
    }

    /**
     * {@inheritDoc}
     */
    public Position getReferencePosition()
    {
        return this.point1;
    }

    /**
     * {@inheritDoc}
     */
    protected void doRenderGraphic(DrawContext dc)
    {
        for (Path path : this.paths)
        {
            path.render(dc);
        }
    }

    /**
     * Compute positions and create the paths required to draw graphic.
     *
     * @param dc Current draw context.
     */
    @Override
    protected void computeGeometry(DrawContext dc)
    {
        if (this.paths == null)
        {
            this.paths = new ArrayList<Path>();

            // Create paths for the block
            String code = this.maskedSymbolCode;
            center = LatLon.getCenter(Arrays.asList(point1, point2));

            // Position of the center point
            Position centerPosition = new Position(center, 0);

            if (TacGrpSidc.TSK_BLK.equals(code))
            {
                // Create the paths from point1 to point2
                this.paths.add(createPath(Arrays.asList(this.point1, this.point2)));
                // Create the paths from centerPosition to point3
                this.paths.add(createPath(Arrays.asList(centerPosition, this.point3)));
            }
            else
            {
                // Size the arrow
                LatLon lengthArrow = LatLon.interpolate(0.125, center, point1);
                // Position the arrow
                Position lengthArrowPosition = new Position(lengthArrow, 0);
                length = LatLon.greatCircleDistance(lengthArrowPosition, this.point1);
                lengthDouble = length.getRadians();
                distance = LatLon.greatCircleDistance(center, this.point3);
                distanceDouble = distance.getRadians();
                
                // This is the condition which changing in length arrow proportional to the graphic
                if (lengthDouble * 1.3 > distanceDouble)
                {
                    length = length.divide(4);
                }

                // Angle up and down line of the arrow
                // Angle between point3 and center
                ang = LatLon.greatCircleAzimuth(center, point3);
                Angle arrowTop = ang.add(Angle.fromDegrees(35));
                Angle arrowBot = ang.add(Angle.fromDegrees(-35));
                // position up and down line of arrow
                Position positionLineUpOfArrow = new Position(
                    LatLon.greatCircleEndPosition(centerPosition, arrowTop, length), 0);
                Position positionLineDownOfArrow = new Position(
                    LatLon.greatCircleEndPosition(centerPosition, arrowBot, length), 0);
                // Create the paths from point1 to point2
                this.paths.add(createPath(Arrays.asList(this.point1, this.point2)));
                // Create the paths from centerPosition to point3
                this.paths.add(createPath(Arrays.asList(centerPosition, this.point3)));
                // Create the arrow
                this.paths.add(
                    createPath(Arrays.asList(positionLineDownOfArrow, centerPosition, positionLineUpOfArrow)));
            }
        }
        super.computeGeometry(dc);
    }

    /**
     * {@inheritDoc}
     */
    protected void applyDelegateOwner(Object owner)
    {
        if (this.paths == null)
        {
            return;
        }

        for (Path path : this.paths)
        {
            path.setDelegateOwner(owner);
        }
    }

    /**
     * Create and configure the Path used to render this graphic.
     *
     * @param positions Positions that define the path.
     *
     * @return New path configured with defaults appropriate for this type of graphic.
     */
    protected Path createPath(List<Position> positions)
    {
        Path path = new Path(positions);
        path.setFollowTerrain(true);
        path.setPathType(AVKey.GREAT_CIRCLE);
        path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        path.setDelegateOwner(this.getActiveDelegateOwner());
        path.setAttributes(this.getActiveShapeAttributes());
        return path;
    }
}
