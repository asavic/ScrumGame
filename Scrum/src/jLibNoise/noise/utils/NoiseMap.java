/*
 * jNoiseLib [https://github.com/andrewgp/jLibNoise]
 * Original code from libnoise [https://github.com/andrewgp/jLibNoise]
 *
 * Copyright (C) 2003, 2004 Jason Bevins
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License (COPYING.txt) for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * The developer's email is jlbezigvins@gmzigail.com (for great email, take
 * off every 'zig'.)
 */

package jLibNoise.noise.utils;

import jLibNoise.noise.ExceptionInvalidParam;
import jLibNoise.noise.ExceptionOutOfMemory;


/**
 * Implements a noise map, a 2-dimensional array of floating-point values.
 * A noise map is designed to store coherent-noise values generated by a
 * noise module, although it can store values from any source.  A noise
 * map is often used as a terrain height map or a grayscale texture.
 * <p/>
 * The size (width and height) of the noise map can be specified during
 * object construction or at any other time.
 * <p/>
 * The GetValue() and SetValue() methods can be used to access individual
 * values stored in the noise map.
 * <p/>
 * This class manages its own memory.  If you copy a noise map object
 * into another noise map object, the original contents of the noise map
 * object will be freed.
 * <p/>
 * If you specify a new size for the noise map and the new size is
 * smaller than the current size, the allocated memory will not be
 * reallocated.
 * Call ReclaimMem() to reclaim the wasted memory.
 * <p/>
 * <b>Border Values</b>
 * <p/>
 * All of the values outside of the noise map are assumed to have a
 * common value known as the <i>border value</i>.
 * <p/>
 * To set the border value, call the SetBorderValue() method.
 * <p/>
 * The GetValue() method returns the border value if the specified value
 * lies outside of the noise map.
 * <p/>
 * <b>Internal Noise Map Structure</b>
 * <p/>
 * Internally, the values are organized into horizontal rows called @a
 * slabs.  Slabs are ordered from bottom to top.
 * <p/>
 * Each slab contains a contiguous row of values in memory.  The values
 * in a slab are organized left to right.
 * <p/>
 * The offset between the starting points of any two adjacent slabs is
 * called the <i>stride amount</i>.  The stride amount is measured by
 * the number of @a float values between these two starting points, not
 * by the number of bytes.  For efficiency reasons, the stride is often a
 * multiple of the machine word size.
 * <p/>
 * The GetSlabPtr() and GetConstSlabPtr() methods allow you to retrieve
 * pointers to the slabs themselves.
 *
 * @source 'noiseutils.h/cpp'
 */
public class NoiseMap {

    // The maximum width of a raster.
    public static int RASTER_MAX_WIDTH = 32767;
    // The maximum height of a raster.
    public static int RASTER_MAX_HEIGHT = 32767;
    // The raster's stride length must be a multiple of this constant.
    public static int RASTER_STRIDE_BOUNDARY = 4;

    // Value used for all positions outside of the noise map.
    private float m_borderValue;
    // The current height of the noise map.
    private int m_height;
    /// The amount of memory allocated for this noise map.
    /// This value is equal to the number of @a float values allocated for
    /// the noise map, not the number of bytes.
    private long m_memUsed;
    /// A pointer to the noise map buffer.
    private float[] m_pNoiseMap;
    // The stride amount of the noise map.
    private int m_stride;
    // The current width of the noise map.
    private int m_width;
    
    public NoiseMap() {
        InitObj();
    }

    /**
     * Creates a noise map with uninitialized values.
     * <p/>
     * It is considered an error if the specified dimensions are not positive.
     *
     * @param width  The width of the new noise map.
     * @param height The height of the new noise map.
     * @throws jLibNoise.noise.ExceptionInvalidParam
     *          See the preconditions.
     * @throws jLibNoise.noise.ExceptionOutOfMemory
     *          Out of memory.
     * @pre The width and height values are positive.
     * @pre The width and height values do not exceed the maximum
     * possible width and height for the noise map.
     */
    public NoiseMap(int width, int height) {
        InitObj();
        SetSize(width, height);
    }

    /**
     * Copy constructor.
     *
     * @throws jLibNoise.noise.ExceptionOutOfMemory
     *          Out of memory.
     */
    public NoiseMap(NoiseMap rhs) {
        InitObj();
        CopyNoiseMap(rhs);
    }

    /// Assignment operator.
    ///
    /// @throw noise::ExceptionOutOfMemory Out of memory.
    ///
    /// @returns Reference to self.
    ///
    /// Creates a copy of the noise map.
//    NoiseMap& operator= (const NoiseMap& rhs);

    /**
     * Clears the noise map to a specified value.
     *
     * @param value The value that all positions within the noise map are cleared to.
     */
    public void Clear(float value) {
        if (m_pNoiseMap != null) {
            for (int i = 0; i < m_height * m_width; i++) {
               m_pNoiseMap[i] = value;
            }
        }
    }

    /**
     * Returns the value used for all positions outside of the noise map.
     * <p/>
     * All positions outside of the noise map are assumed to have a
     * common value known as the <i>border value</i>.
     *
     * @return The value used for all positions outside of the noise map.
     */
    public float GetBorderValue() {
        return m_borderValue;
    }

    /**
     * Returns a const pointer to a slab.
     *
     * @return A const pointer to a slab at the position (0, 0), or
     *         NULL if the noise map is empty.
     */
    public float[] GetConstSlabPtr() {
        return m_pNoiseMap;
    }

    /**
     * Returns a const pointer to a slab at the specified row.
     * <p/>
     * This method does not perform bounds checking so be careful when calling it.
     *
     * @param row The row, or @a y coordinate.
     * @return A const pointer to a slab at the position ( 0, @a row ), or @a NULL if the noise map is empty.
     * @pre The coordinates must exist within the bounds of the noise map.
     */
    public ArrayPointer.NativeFloatPrim GetConstSlabPtr(int row) {
        return GetConstSlabPtr(0, row);
    }

    /**
     * Returns a const pointer to a slab at the specified position.
     * <p/>
     * This method does not perform bounds checking so be careful when calling it.
     *
     * @param x The x coordinate of the position.
     * @param y The y coordinate of the position.
     * @return A const pointer to a slab at the position ( @a x, @a y ), or @a NULL if the noise map is empty.
     * @pre The coordinates must exist within the bounds of the noise map.
     */
    public ArrayPointer.NativeFloatPrim GetConstSlabPtr(int x, int y) {
//        return m_pNoiseMap + (long) x + (long) m_stride * (long) y;
        return new ArrayPointer.NativeFloatPrim(m_pNoiseMap, x + (y * m_width));
    }

    /**
     * Returns the height of the noise map.
     *
     * @return The height of the noise map.
     */
    public int GetHeight() {
        return m_height;
    }

    /**
     * Returns the amount of memory allocated for this noise map.
     * <p/>
     * This method returns the number of @a float values allocated.
     *
     * @return The amount of memory allocated for this noise map.
     */
    public long GetMemUsed() {
        return m_memUsed;
    }

    /**
     * Returns a pointer to a slab.
     *
     * @return A pointer to a slab at the position (0, 0), or @a NULL if the noise map is empty.
     */
    public float[] GetSlabPtr() {
        return m_pNoiseMap;
    }

    /**
     * Returns a pointer to a slab at the specified row.
     * <p/>
     * This method does not perform bounds checking so be careful when calling it.
     *
     * @param row The row, or @a y coordinate.
     * @return A pointer to a slab at the position ( 0, @a row ), or @a NULL if the noise map is empty.
     * @pre The coordinates must exist within the bounds of the noise map.
     */
    public ArrayPointer.NativeFloatPrim GetSlabPtr(int row) {
        return GetSlabPtr(0, row);
    }

    /**
     * Returns a pointer to a slab at the specified position.
     * <p/>
     * This method does not perform bounds checking so be careful when calling it.
     *
     * @param x The x coordinate of the position.
     * @param y The y coordinate of the position.
     * @return A pointer to a slab at the position ( @a x, @a y ) or @a NULL if the noise map is empty.
     * @pre The coordinates must exist within the bounds of the noise map.
     */
    public ArrayPointer.NativeFloatPrim GetSlabPtr(int x, int y) {
//        return m_pNoiseMap + (long) x + (long) m_stride * (long) y;
        return new ArrayPointer.NativeFloatPrim(m_pNoiseMap, x * y);
    }

    /**
     * Returns the stride amount of the noise map.
     * <p/>
     * - The <i>stride amount</i> is the offset between the starting
     * points of any two adjacent slabs in a noise map.
     * - The stride amount is measured by the number of @a float values
     * between these two points, not by the number of bytes.
     *
     * @return The stride amount of the noise map.
     */
    public int GetStride() {
        return m_stride;
    }

    /**
     * Returns a value from the specified position in the noise map.
     * <p/>
     * This method returns the border value if the coordinates exist outside of the noise map.
     *
     * @param x The x coordinate of the position.
     * @param y The y coordinate of the position.
     * @return The value at that position.
     */
    public float GetValue(int x, int y) {
        if (m_pNoiseMap != null) {
            if (x >= 0 && x < m_width && y >= 0 && y < m_height) {
//                return*(GetConstSlabPtr(x, y));
                throw new UnsupportedOperationException();
            }
        }
        // The coordinates specified are outside the noise map.  Return the border
        // value.
        return m_borderValue;
    }

    /**
     * Returns the width of the noise map.
     *
     * @return The width of the noise map.
     */
    public int GetWidth() {
        return m_width;
    }

    /**
     * Reallocates the noise map to recover wasted memory.
     * <p/>
     * The contents of the noise map is unaffected.
     *
     * @throw ExceptionOutOfMemory Out of memory.  (Yes, this
     * method can return an out-of-memory exception because two noise
     * maps will temporarily exist in memory during this call.)
     */
    public void ReclaimMem() {
//        size_t newMemUsage = CalcMinMemUsage(m_width, m_height);
//        if (m_memUsed > newMemUsage) {
//            // There is wasted memory.  Create the smallest buffer that can fit the
//            // data and copy the data to it.
//            float*pNewNoiseMap = NULL;
//            try {
//                pNewNoiseMap = new float[newMemUsage];
//            } catch (...){
//                throw noise::ExceptionOutOfMemory();
//            }
//            memcpy(pNewNoiseMap, m_pNoiseMap, newMemUsage * sizeof(float));
//            delete[] m_pNoiseMap;
//            m_pNoiseMap = pNewNoiseMap;
//            m_memUsed = newMemUsage;
//        }
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the value to use for all positions outside of the noise map.
     * <p/>
     * All positions outside of the noise map are assumed to have a common value known as the <i>border value</i>.
     *
     * @param borderValue The value to use for all positions outside of the noise map.
     */
    public void SetBorderValue(float borderValue) {
        m_borderValue = borderValue;
    }

    /**
     * Sets the new size for the noise map.
     * <p/>
     * On exit, the contents of the noise map are undefined.
     * If the @a OUT_OF_MEMORY exception occurs, this noise map object becomes empty.
     * If the @a INVALID_PARAM exception occurs, the noise map is unmodified.
     *
     * @param width  The new width for the noise map.
     * @param height The new height for the noise map.
     * @throws jLibNoise.noise.ExceptionInvalidParam
     *          See the preconditions.
     * @throws jLibNoise.noise.ExceptionOutOfMemory
     *          Out of memory.
     * @pre The width and height values are positive.
     * @pre The width and height values do not exceed the maximum possible width and height for the noise map.
     */
    public void SetSize(int width, int height) {
        if (width < 0 || height < 0
                || width > RASTER_MAX_WIDTH || height > RASTER_MAX_HEIGHT) {
            // Invalid width or height.
            throw new ExceptionInvalidParam();
        } else if (width == 0 || height == 0) {
            // An empty noise map was specified.  Delete it and zero out the size
            // member variables.
            DeleteNoiseMapAndReset();
        } else {
            // A new noise map size was specified.  Allocate a new noise map buffer
            // unless the current buffer is large enough for the new noise map (we
            // don't want costly reallocations going on.)
            long newMemUsage = CalcMinMemUsage(width, height);
            if (m_memUsed < newMemUsage) {
                // The new size is too big for the current noise map buffer.  We need to
                // reallocate.
                DeleteNoiseMapAndReset();
                try {
                    m_pNoiseMap = new float[(int) newMemUsage];
                } catch (Exception e) {
                    throw new ExceptionOutOfMemory();
                }
                m_memUsed = newMemUsage;
            }
            m_stride = (int) CalcStride(width);
            m_width = width;
            m_height = height;
        }
    }

    /**
     * Sets a value at a specified position in the noise map.
     * <p/>
     * This method does nothing if the noise map object is empty or the position is outside the bounds of the noise map.
     *
     * @param x     The x coordinate of the position.
     * @param y     The y coordinate of the position.
     * @param value The value to set at the given position.
     */
    public void SetValue(int x, int y, float value) {
        if (m_pNoiseMap != null) {
            if (x >= 0 && x < m_width && y >= 0 && y < m_height) {
//                *(GetSlabPtr(x, y)) = value;
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Takes ownership of the buffer within the source noise map.
     * <p/>
     * On exit, the source noise map object becomes empty.
     * <p/>
     * This method only moves the buffer pointer so this method is very quick.
     *
     * @param source The source noise map.
     */
    public void TakeOwnership(NoiseMap source) {
        // Copy the values and the noise map buffer from the source noise map to
        // this noise map.  Now this noise map pwnz the source buffer.
//        delete[] m_pNoiseMap;
        m_memUsed = source.m_memUsed;
        m_height = source.m_height;
        m_pNoiseMap = source.m_pNoiseMap;
        m_stride = source.m_stride;
        m_width = source.m_width;

        // Now that the source buffer is assigned to this noise map, reset the
        // source noise map object.
        source.InitObj();
    }

    /**
     * Returns the minimum amount of memory required to store a noise map of the specified size.
     * <p/>
     * The returned value is measured by the number of @a float values
     * required to store the noise map, not by the number of bytes.
     *
     * @param width  The width of the noise map.
     * @param height The height of the noise map.
     * @return The minimum amount of memory required to store the noise map.
     */
    private long CalcMinMemUsage(int width, int height) {
        return CalcStride(width * height);
    }

    /**
     * Calculates the stride amount for a noise map.
     * <p/>
     * - The <i>stride amount</i> is the offset between the starting
     * points of any two adjacent slabs in a noise map.
     * - The stride amount is measured by the number of @a float values
     * between these two points, not by the number of bytes.
     *
     * @param width The width of the noise map.
     * @return The stride amount.
     */
    private long CalcStride(int width) {
        return width;//(long) (((width + RASTER_STRIDE_BOUNDARY - 1) / RASTER_STRIDE_BOUNDARY) * RASTER_STRIDE_BOUNDARY);
    }

    /**
     * Copies the contents of the buffer in the source noise map into this noise map.
     *
     * This method reallocates the buffer in this noise map object if necessary.
     *
     * @param source The source noise map.
     * @throws jLibNoise.noise.ExceptionOutOfMemory Out of memory.
     *
     * @warning This method calls the standard library function
     * @a memcpy, which probably violates the DMCA because it can be used
     * to make a bitwise copy of anything, like, say, a DVD.  Don't call
     * this method if you live in the USA.
     */
    private void CopyNoiseMap (NoiseMap source) {
//        // Resize the noise map buffer, then copy the slabs from the source noise
//        // map buffer to this noise map buffer.
//        SetSize(source.GetWidth(), source.GetHeight());
//        for (int y = 0; y < source.GetHeight(); y++) {
//            const float*pSource = source.GetConstSlabPtr(0, y);
//            float*pDest = GetSlabPtr(0, y);
//            memcpy(pDest, pSource, (size_t) source.GetWidth() * sizeof(float));
//        }
//
//        // Copy the border value as well.
//        m_borderValue = source.m_borderValue;
        throw new UnsupportedOperationException();
    }

    /**
     * Resets the noise map object.
     * <p/>
     * This method is similar to the InitObj() method, except this method deletes the buffer in this noise map.
     */
    private void DeleteNoiseMapAndReset() {
//        delete[] m_pNoiseMap;
        InitObj();
    }

    /**
     * Initializes the noise map object.
     *
     * @pre Must be called during object construction.
     * @pre The noise map buffer must not exist.
     */
    private void InitObj() {
        m_pNoiseMap = null;
        m_height = 0;
        m_width = 0;
        m_stride = 0;
        m_memUsed = 0;
        m_borderValue = 0.0f;
    }
}
