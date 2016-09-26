/*
 * Copyright 2014 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.checkout;

import android.content.Context;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

public final class RobotmediaInventory extends BaseInventory {

    private class MyTask extends Task {
        public MyTask(@Nonnull Request request, @Nonnull Callback callback) {
            super(request, callback);
        }

        @Override
        public void run() {
            if (RobotmediaDatabase.exists(mCheckout.getContext())) {
                mBackground.execute(new Loader());
            } else {
                onLoaded(RobotmediaDatabase.toInventoryProducts(ProductTypes.ALL));
            }
        }

        private class Loader implements Runnable {
            @Override
            public void run() {
                final Context context = mCheckout.getContext();
                final RobotmediaDatabase database = new RobotmediaDatabase(context);
                final Products products = database.load(mRequest);
                onLoaded(products);
            }
        }

        private void onLoaded(Products products) {
            synchronized (mLock) {
                mProducts.merge(products);
            }
            mOnLoadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    onDone();
                }
            });
        }
    }

    @Nonnull
    private final Executor mBackground;
    @Nonnull
    private final Executor mOnLoadExecutor;

    public RobotmediaInventory(@Nonnull Checkout checkout, @Nonnull Executor onLoadExecutor) {
        this(checkout, Executors.newSingleThreadExecutor(), onLoadExecutor);
    }

    public RobotmediaInventory(@Nonnull Checkout checkout, @Nonnull Executor background,
            @Nonnull Executor onLoadExecutor) {
        super(checkout);
        mBackground = background;
        mOnLoadExecutor = onLoadExecutor;
    }


    @Override
    protected Task createTask(@Nonnull Request request, @Nonnull Callback callback) {
        return new MyTask(request, callback);
    }
}
