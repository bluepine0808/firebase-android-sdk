// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.firebase.gradle.plugins.measurement.utils.enums

import com.google.gson.annotations.SerializedName

enum TableName {
    @SerializedName('PullRequests')
    PULL_REQUESTS,

    @SerializedName('ApkSizes')
    APK_SIZES,

    @SerializedName('Coverage1')
    IOS_COVERAGE,

    @SerializedName('Coverage2')
    ANDROID_COVERAGE

    @Override
    String toString() {
        TableName.getField(this.name())
                .getAnnotation(SerializedName.class).value()
    }

}
