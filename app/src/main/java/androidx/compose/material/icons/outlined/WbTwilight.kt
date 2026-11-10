/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.WbTwilight: ImageVector
    get() {
        if (_wbTwilight != null) {
            return _wbTwilight!!
        }
        _wbTwilight = materialIcon(name = "Outlined.WbTwilight") {
            materialPath {
                moveTo(16.955f, 8.662f)
                lineToRelative(2.121f, -2.122f)
                lineToRelative(1.415f, 1.414f)
                lineToRelative(-2.121f, 2.122f)
                close()
            }
            materialPath {
                moveTo(2.0f, 18.0f)
                horizontalLineToRelative(20.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-20.0f)
                close()
            }
            materialPath {
                moveTo(11.0f, 4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(-2.0f)
                close()
            }
            materialPath {
                moveTo(3.543f, 7.925f)
                lineToRelative(1.414f, -1.414f)
                lineToRelative(2.121f, 2.121f)
                lineToRelative(-1.414f, 1.414f)
                close()
            }
            materialPath {
                moveTo(5.0f, 16.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(0.0f, -3.87f, -3.13f, -7.0f, -7.0f, -7.0f)
                reflectiveCurveTo(5.0f, 12.13f, 5.0f, 16.0f)
                close()
            }
        }
        return _wbTwilight!!
    }

private var _wbTwilight: ImageVector? = null
