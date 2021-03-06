/**
 * Copyright (c) 2015 Basho Technologies, Inc.
 *
 * This file is provided to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.basho.riak.spark.rdd

import com.basho.riak.client.core.query.Namespace

/**
 * Will be later extended with the partition information
 */
case class BucketDef(bucketType: String,
                    bucketName: String){

  def asNamespace(): Namespace = new Namespace(bucketType, bucketName)
}

object BucketDef{
  def apply(bucketName: String): BucketDef = new BucketDef("default", bucketName)
  def apply(ns: Namespace): BucketDef = new BucketDef(ns.getBucketTypeAsString, ns.getBucketNameAsString)
}
