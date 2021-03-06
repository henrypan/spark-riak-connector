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
package com.basho.riak.spark.japi.rdd;

import com.basho.riak.spark.rdd.RiakRDD;
import com.basho.riak.spark.util.JavaApiHelper;
import org.apache.spark.api.java.JavaRDD;
import static com.basho.riak.spark.util.JavaApiHelper.getClassTag;

import scala.reflect.ClassTag;

public class RiakJavaRDD<T> extends JavaRDD<T> {
    private ClassTag<T> classTag;

    public RiakJavaRDD(RiakRDD<T> rdd, Class<T> clazz) {
        this(rdd, getClassTag(clazz));
    }

    public RiakJavaRDD(RiakRDD<T> rdd, ClassTag<T> classTag) {
        super(rdd, classTag);
        this.classTag = classTag;
    }

    @Override
    public RiakRDD<T> rdd() {
        return (RiakRDD<T>) super.rdd();
    }

    private RiakJavaRDD<T> wrap(RiakRDD<T> newRDD) {
        return new RiakJavaRDD<>(newRDD, classTag());
    }

    @Override
    public ClassTag<T> classTag() {
        return classTag;
    }

    public RiakJavaRDD<T> query2iRange(String index, Long from, Long to){
        return wrap(rdd().query2iRange(index, from, to));
    }

    public RiakJavaRDD<T> queryBucketKeys(String... keys){
        return wrap(rdd().queryBucketKeys(JavaApiHelper.toScalaSeq(keys)));
    }

    public RiakJavaRDD<T> queryAll(){
        return wrap(rdd().queryAll());
    }
}
