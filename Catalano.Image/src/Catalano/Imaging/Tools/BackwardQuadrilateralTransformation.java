// Catalano Imaging Library
// The Catalano Framework
//
// Copyright © Diego Catalano, 2015
// diego.catalano at live.com
//
// Copyright © Andrew Kirillov, 2007-2008
// andrew.kirillov at gmail.com
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
//

package Catalano.Imaging.Tools;

import java.util.ArrayList;

import Catalano.Core.IntPoint;
import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.FastBitmap.ColorSpace;
import Catalano.Imaging.IProcessImage;
import Catalano.Math.Geometry.PointsCloud;
import Catalano.Math.Geometry.QuadrilateralTransformationCalc;

/**
 * Performs backward quadrilateral transformation of a given source image.
 * 
 * The class implements backward quadrilateral transformation algorithm, which allows to transform any rectangular image into any quadrilateral.
 * Basically it is a backward implementation of #QuadrilateralTransformation
 * The idea of the algorithm is based on homogeneous transformation and its math is described by Paul Heckbert in his "Projective Mappings for Image Warping" paper.
 * 
 * @author Diego Catalano
 * @author Martin Jess
 * 
 */
public class BackwardQuadrilateralTransformation implements IProcessImage{
    
    private boolean useInterpolation = true;
    private int newWidth = -1;
    private int newHeight = - 1;
    private ArrayList<IntPoint> destinationQuadrilateral;


    /**
     * Initializes a new instance of the BackwardQuadrilateralTransformation class.
     * @param destinationQuadrilateral Quadrilateral's corners.
     */
    public BackwardQuadrilateralTransformation(ArrayList<IntPoint> destinationQuadrilateral){       
        this.destinationQuadrilateral = destinationQuadrilateral;  
    }
    
    /**
     * Initializes a new instance of the BackwardQuadrilateralTransformation class.
     * @param destinationQuadrilateral Quadrilateral's corners.
     * @param newWidth new width of the resulting image.
     * @param newHeight new height of the resulting image.
     */
    public BackwardQuadrilateralTransformation(ArrayList<IntPoint> destinationQuadrilateral, int newWidth, int newHeight){
        this.destinationQuadrilateral = destinationQuadrilateral;
        this.newWidth  = newWidth;
        this.newHeight = newHeight;
    }
    
    
    /**
     * Get Quadrilateral's corners in destination image.
     * @return Quadrilateral's corners.
     */
    public ArrayList<IntPoint> getDestinationQuadrilateral() {
        return destinationQuadrilateral;
    }

    /**
     * Set Quadrilateral's corners in destination image.
     * @param destinationQuadrilateral Quadrilateral's corners.
     */
    public void setDestinationQuadrilateral(ArrayList<IntPoint> destinationQuadrilateral) {
        this.destinationQuadrilateral = destinationQuadrilateral;
    }

  
    /**
     * Get New width.
     * @return New width.
     */
    public int getNewWidth() {
        return newWidth;
    }

    /**
     * Set New width.
     * @param newWidth New width.
     */
    public void setNewWidth(int newWidth) {
        this.newWidth = newWidth;       
    }

    /**
     * Get New height.
     * @return New height.
     */
    public int getNewHeight() {
        return newHeight;
    }

    /**
     * Set New height.
     * @param newHeight New height.
     */
    public void setNewHeight(int newHeight) {
        this.newHeight = newHeight;       
    }
    
    /**
     * Specifies if bilinear interpolation should be used or not.
     * @return True or false.
     */
    public boolean isUseInterpolation() {
        return useInterpolation;
    }

    /**
     * Set if bilinear interpolation should be used or not.
     * @param useInterpolation True or false.
     */
    public void setUseInterpolation(boolean useInterpolation) {
        this.useInterpolation = useInterpolation;
    }
    
  
    
    @Override
    public FastBitmap ProcessImage(FastBitmap fastBitmap) {
       
    	FastBitmap dst = new FastBitmap(newWidth, newHeight, fastBitmap.getColorSpace());
                         
        int srcWidth = fastBitmap.getWidth();
        int srcHeight = fastBitmap.getHeight();

        //if new width and/or new height was not set use the one from the source image
        int dstHeight = (newHeight == -1) ? fastBitmap.getHeight() : newHeight;
        int dstWidth = (newWidth == -1) ? fastBitmap.getWidth() : newWidth;

        //FIXME not sure if this could be done better, initialize complete image with transparency in case of ARGB
        if (fastBitmap.isARGB()) {
        	for (int x = 0; x < dstHeight; x++) {
        		for (int y = 0; y < dstWidth; y++) {
        			dst.setARGB(x, y, 0, 0, 0, 0);
        		}
        	}
        }

        ArrayList<IntPoint> srcRect = new ArrayList<IntPoint>( );
        srcRect.add( new IntPoint( 0, 0 ) );
        srcRect.add( new IntPoint( srcWidth - 1, 0 ) );
        srcRect.add( new IntPoint( srcWidth - 1, srcHeight - 1 ) );
        srcRect.add( new IntPoint( 0, srcHeight - 1 ) );
        
        // get bounding rectangle of the quadrilateral
        IntPoint minXY, maxXY;
        ArrayList<IntPoint> list = PointsCloud.GetBoundingRectangle( destinationQuadrilateral);
        minXY = list.get(0);
        maxXY = list.get(1);

        // make sure the rectangle is inside of destination image
        if ( ( maxXY.x < 0 ) || ( maxXY.y < 0 ) || ( minXY.x >= dstWidth ) || ( minXY.y >= dstHeight ) )
            return null; // nothing to do, since quadrilateral is completely outside

        // correct rectangle if required
        if ( minXY.x < 0 )
            minXY.x = 0;
        if ( minXY.y < 0 )
            minXY.y = 0;
        if ( maxXY.x >= dstWidth )
            maxXY.x = dstWidth - 1;
        if ( maxXY.y >= dstHeight )
            maxXY.y = dstHeight - 1;

        int startX = minXY.x;
        int startY = minXY.y;
        int stopX  = maxXY.x + 1;
        int stopY  = maxXY.y + 1;
        //int offset = dstStride - ( stopX - startX ) * pixelSize;

        // calculate tranformation matrix
        double[][] matrix = QuadrilateralTransformationCalc.MapQuadToQuad( destinationQuadrilateral, srcRect );
        
        if(!useInterpolation){
            if (fastBitmap.isRGB()){
            	// for each row
                for ( int y = startY; y < stopY; y++ )
                {
                    // for each pixel
                    for ( int x = startX; x < stopX; x++ )
                    {
                        double factor = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2];
                        double srcX = ( matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] ) / factor;
                        double srcY = ( matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] ) / factor;

                        if ( ( srcX >= 0 ) && ( srcY >= 0 ) && ( srcX < srcWidth ) && ( srcY < srcHeight ) )
                        {
                            int r = fastBitmap.getRed((int)srcY, (int)srcX);
                            int g = fastBitmap.getGreen((int)srcY, (int)srcX);
                            int b = fastBitmap.getBlue((int)srcY, (int)srcX);
                            dst.setRGB(y, x, r, g, b);
                        }
                    }
                }
            }
            else if (fastBitmap.isARGB()){
            	// for each row
                for ( int y = startY; y < stopY; y++ )
                {
                    // for each pixel
                    for ( int x = startX; x < stopX; x++ )
                    {
                        double factor = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2];
                        double srcX = ( matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] ) / factor;
                        double srcY = ( matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] ) / factor;

                        if ( ( srcX >= 0 ) && ( srcY >= 0 ) && ( srcX < srcWidth ) && ( srcY < srcHeight ) )
                        {
                        	int a = fastBitmap.getAlpha((int)srcY, (int)srcX);                        			
                            int r = fastBitmap.getRed((int)srcY, (int)srcX);
                            int g = fastBitmap.getGreen((int)srcY, (int)srcX);
                            int b = fastBitmap.getBlue((int)srcY, (int)srcX);
                            dst.setARGB(y, x, a, r, g, b);
                        }
                    }
                }
            }
            else if (fastBitmap.isGrayscale()){
            	// for each row
                for ( int y = startY; y < stopY; y++ )
                {
                    // for each pixel
                    for ( int x = startX; x < stopX; x++ )
                    {
                        double factor = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2];
                        double srcX = ( matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] ) / factor;
                        double srcY = ( matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] ) / factor;

                        if ( ( srcX >= 0 ) && ( srcY >= 0 ) && ( srcX < srcWidth ) && ( srcY < srcHeight ) )
                        {
                            int g = fastBitmap.getGray((int)srcY, (int)srcX);
                            dst.setGray(y, x, g);
                        }
                    }
                }
            }
        }
        else{
            if (fastBitmap.isRGB()){
                int srcWidthM1  = srcWidth - 1;
                int srcHeightM1 = srcHeight - 1;

                // coordinates of source points
                double dx1, dy1, dx2, dy2;
                int sx1, sy1, sx2, sy2;
                
                int p1r, p2r, p3r, p4r;
                int p1g, p2g, p3g, p4g;
                int p1b, p2b, p3b, p4b;
                
                // for each row
                for ( int y = startY; y < stopY; y++ )
                {
                    // for each pixel
                    for ( int x = startX; x < stopX; x++ )
                    {
                        double factor = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2];
                        double srcX = ( matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] ) / factor;
                        double srcY = ( matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] ) / factor;
                        
                        if ( ( srcX >= 0 ) && ( srcY >= 0 ) && ( srcX < srcWidth ) && ( srcY < srcHeight ) ){
                            sx1 = (int) srcX;
                            sx2 = ( sx1 == srcWidthM1 ) ? sx1 : sx1 + 1;
                            dx1 = srcX - sx1;
                            dx2 = 1.0 - dx1;

                            sy1 = (int) srcY;
                            sy2 = ( sy1 == srcHeightM1 ) ? sy1 : sy1 + 1;
                            dy1 = srcY - sy1;
                            dy2 = 1.0 - dy1;

                            // get four points in Red channel
                            p1r = fastBitmap.getRed(sy1, sx1);
                            p2r = fastBitmap.getRed(sy1, sx2);
                            p3r = fastBitmap.getRed(sy2, sx1);
                            p4r = fastBitmap.getRed(sy2, sx2);
                            
                            // get four points in Green channel
                            p1g = fastBitmap.getGreen(sy1, sx1);
                            p2g = fastBitmap.getGreen(sy1, sx2);
                            p3g = fastBitmap.getGreen(sy2, sx1);
                            p4g = fastBitmap.getGreen(sy2, sx2);
                            
                            // get four points in Blue channel
                            p1b = fastBitmap.getBlue(sy1, sx1);
                            p2b = fastBitmap.getBlue(sy1, sx2);
                            p3b = fastBitmap.getBlue(sy2, sx1);
                            p4b = fastBitmap.getBlue(sy2, sx2);
                                                                                   
                            int r = (int)(dy2 * (dx2 * (p1r) + dx1 * (p2r)) + dy1 * (dx2 * (p3r) + dx1 * (p4r)));
                            int g = (int)(dy2 * (dx2 * (p1g) + dx1 * (p2g)) + dy1 * (dx2 * (p3g) + dx1 * (p4g)));
                            int b = (int)(dy2 * (dx2 * (p1b) + dx1 * (p2b)) + dy1 * (dx2 * (p3b) + dx1 * (p4b)));
                            
                            dst.setRGB(y, x, r, g, b);                            
                        }                       
                    }
                }
            }
            
            else if (fastBitmap.isARGB()){
                int srcWidthM1  = srcWidth - 1;
                int srcHeightM1 = srcHeight - 1;

                // coordinates of source points
                double dx1, dy1, dx2, dy2;
                int sx1, sy1, sx2, sy2;
                
                int p1r, p2r, p3r, p4r;
                int p1g, p2g, p3g, p4g;
                int p1b, p2b, p3b, p4b;
                int p1a, p2a, p3a, p4a;
                
                // for each row
                for ( int y = startY; y < stopY; y++ )
                {
                    // for each pixel
                    for ( int x = startX; x < stopX; x++ )
                    {
                        double factor = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2];
                        double srcX = ( matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] ) / factor;
                        double srcY = ( matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] ) / factor;
                        
                        if ( ( srcX >= 0 ) && ( srcY >= 0 ) && ( srcX < srcWidth ) && ( srcY < srcHeight ) ){
                            sx1 = (int) srcX;
                            sx2 = ( sx1 == srcWidthM1 ) ? sx1 : sx1 + 1;
                            dx1 = srcX - sx1;
                            dx2 = 1.0 - dx1;

                            sy1 = (int) srcY;
                            sy2 = ( sy1 == srcHeightM1 ) ? sy1 : sy1 + 1;
                            dy1 = srcY - sy1;
                            dy2 = 1.0 - dy1;
                            
                            // get four points in Alpha channel
                            p1a = fastBitmap.getAlpha(sy1, sx1);
                            p2a = fastBitmap.getAlpha(sy1, sx2);
                            p3a = fastBitmap.getAlpha(sy2, sx1);
                            p4a = fastBitmap.getAlpha(sy2, sx2);

                            // get four points in Red channel
                            p1r = fastBitmap.getRed(sy1, sx1);
                            p2r = fastBitmap.getRed(sy1, sx2);
                            p3r = fastBitmap.getRed(sy2, sx1);
                            p4r = fastBitmap.getRed(sy2, sx2);
                            
                            // get four points in Green channel
                            p1g = fastBitmap.getGreen(sy1, sx1);
                            p2g = fastBitmap.getGreen(sy1, sx2);
                            p3g = fastBitmap.getGreen(sy2, sx1);
                            p4g = fastBitmap.getGreen(sy2, sx2);
                            
                            // get four points in Blue channel
                            p1b = fastBitmap.getBlue(sy1, sx1);
                            p2b = fastBitmap.getBlue(sy1, sx2);
                            p3b = fastBitmap.getBlue(sy2, sx1);
                            p4b = fastBitmap.getBlue(sy2, sx2);
                            
                           
                            int a = (int)(dy2 * (dx2 * (p1a) + dx1 * (p2a)) + dy1 * (dx2 * (p3a) + dx1 * (p4a)));                                                       
                            int r = (int)(dy2 * (dx2 * (p1r) + dx1 * (p2r)) + dy1 * (dx2 * (p3r) + dx1 * (p4r)));
                            int g = (int)(dy2 * (dx2 * (p1g) + dx1 * (p2g)) + dy1 * (dx2 * (p3g) + dx1 * (p4g)));
                            int b = (int)(dy2 * (dx2 * (p1b) + dx1 * (p2b)) + dy1 * (dx2 * (p3b) + dx1 * (p4b)));
                            
                            dst.setARGB(y, x, a, r, g, b);                            
                        }                       
                    }
                }
            }
            
            else if (fastBitmap.isGrayscale()){
                int srcWidthM1  = srcWidth - 1;
                int srcHeightM1 = srcHeight - 1;

                // coordinates of source points
                double dx1, dy1, dx2, dy2;
                int sx1, sy1, sx2, sy2;
                
                int p1, p2, p3, p4;
                
                for (int i = 0; i < dstHeight; i++) {
                    for (int j = 0; j < dstWidth; j++) {
                        double factor = matrix[2][0] * j + matrix[2][1] * i + matrix[2][2];
                        double srcX = ( matrix[0][0] * j + matrix[0][1] * i + matrix[0][2] ) / factor;
                        double srcY = ( matrix[1][0] * j + matrix[1][1] * i + matrix[1][2] ) / factor;
                        
                        if ( ( srcX >= 0 ) && ( srcY >= 0 ) && ( srcX < srcWidth ) && ( srcY < srcHeight ) ){
                            sx1 = (int) srcX;
                            sx2 = ( sx1 == srcWidthM1 ) ? sx1 : sx1 + 1;
                            dx1 = srcX - sx1;
                            dx2 = 1.0 - dx1;

                            sy1 = (int) srcY;
                            sy2 = ( sy1 == srcHeightM1 ) ? sy1 : sy1 + 1;
                            dy1 = srcY - sy1;
                            dy2 = 1.0 - dy1;

                            // get four points
                            p1 = fastBitmap.getGray(sy1, sx1);
                            p2 = fastBitmap.getGray(sy1, sx2);
                            p3 = fastBitmap.getGray(sy2, sx1);
                            p4 = fastBitmap.getGray(sy2, sx2);
                            
                            int g = (int)(dy2 * (dx2 * (p1) + dx1 * (p2)) + dy1 * (dx2 * (p3) + dx1 * (p4)));
                            dst.setGray(i, j, g);
                        }
                        
                    }
                }
            }
        }
        
        return dst;
    }
 
}
