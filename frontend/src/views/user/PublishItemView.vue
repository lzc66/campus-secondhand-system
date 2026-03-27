<template>
  <section class="glass-card panel">
    <SectionHeading
      :title="isEdit ? '编辑商品' : '发布商品'"
      description="上传图片、选择分类，并完善价格、成色和联系方式。"
      tag="Publish"
    />

    <div class="publish-banner">
      <div>
        <strong>{{ isEdit ? '完善商品资料' : '把闲置好物放上交易板' }}</strong>
        <p>商品信息会直接影响列表展示、详情页转化和在线下单体验，建议一次填写完整。</p>
      </div>
      <div class="publish-tips">
        <span>至少上传 1 张图片</span>
        <span>价格必须大于 0</span>
        <span>手机号 / QQ / 微信至少填写 1 项</span>
      </div>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="商品图片" required>
        <div class="image-uploader">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleImageChange"
          >
            <el-button :loading="uploadingImage">上传图片</el-button>
          </el-upload>
          <div class="image-list">
            <div v-for="(image, index) in imageUrls" :key="`${image}-${index}`" class="image-card">
              <img :src="image" alt="item" />
              <button type="button" class="remove-btn" @click="removeImage(index)">×</button>
              <span v-if="index === 0" class="cover-badge">封面</span>
            </div>
          </div>
        </div>
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option
              v-for="item in categories"
              :key="item.categoryId"
              :label="item.categoryName"
              :value="item.categoryId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="form.brand" maxlength="40" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" maxlength="40" />
        </el-form-item>
        <el-form-item label="成色" prop="conditionLevel">
          <el-select v-model="form.conditionLevel">
            <el-option label="全新" value="new" />
            <el-option label="几乎全新" value="almost_new" />
            <el-option label="轻微使用" value="lightly_used" />
            <el-option label="正常使用" value="used" />
            <el-option label="明显使用痕迹" value="well_used" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易方式" prop="tradeMode">
          <el-select v-model="form.tradeMode">
            <el-option label="线上 + 线下" value="both" />
            <el-option label="仅线下" value="offline" />
            <el-option label="仅线上" value="online" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="上架状态" prop="status">
          <el-select v-model="form.status">
            <el-option label="立即上架" value="on_sale" />
            <el-option label="先存为下架" value="off_shelf" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="商品描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="5" maxlength="500" show-word-limit />
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="手机号">
          <el-input v-model="form.contactPhone" maxlength="20" />
        </el-form-item>
        <el-form-item label="QQ">
          <el-input v-model="form.contactQq" maxlength="20" />
        </el-form-item>
        <el-form-item label="微信">
          <el-input v-model="form.contactWechat" maxlength="30" />
        </el-form-item>
        <el-form-item label="取货地址" prop="pickupAddress">
          <el-input v-model="form.pickupAddress" maxlength="120" />
        </el-form-item>
      </div>

      <el-form-item>
        <el-button @click="router.push('/user/my-items')">返回列表</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ isEdit ? '保存修改' : '发布商品' }}
        </el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules, UploadFile } from 'element-plus';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import SectionHeading from '@/components/common/SectionHeading.vue';
import { publicApi } from '@/api/public';
import { userApi } from '@/api/user';

const route = useRoute();
const router = useRouter();
const formRef = ref<FormInstance>();
const categories = ref<any[]>([]);
const imageFileIds = ref<number[]>([]);
const imageUrls = ref<string[]>([]);
const uploadingImage = ref(false);
const submitting = ref(false);

const form = reactive<any>({
  categoryId: undefined,
  title: '',
  brand: '',
  model: '',
  description: '',
  conditionLevel: 'almost_new',
  price: 0,
  originalPrice: 0,
  stock: 1,
  tradeMode: 'both',
  negotiable: true,
  contactPhone: '',
  contactQq: '',
  contactWechat: '',
  pickupAddress: '',
  status: 'on_sale'
});

const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  conditionLevel: [{ required: true, message: '请选择成色', trigger: 'change' }],
  price: [
    { required: true, message: '请输入价格', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value > 0) {
          callback();
          return;
        }
        callback(new Error('价格必须大于 0'));
      },
      trigger: 'change'
    }
  ],
  stock: [{ required: true, message: '请输入库存', trigger: 'change' }],
  tradeMode: [{ required: true, message: '请选择交易方式', trigger: 'change' }],
  pickupAddress: [{ required: true, message: '请输入取货地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择上架状态', trigger: 'change' }]
};

const isEdit = computed(() => Boolean(route.query.editId));

onMounted(async () => {
  categories.value = await publicApi.getCategories();
  if (!isEdit.value) {
    return;
  }
  const detail = await userApi.getMyItemDetail(Number(route.query.editId));
  Object.assign(form, detail, { coverImageFileId: detail.images?.[0]?.fileId });
  imageUrls.value = (detail.images || []).map((item: any) => item.imageUrl);
  imageFileIds.value = (detail.images || []).map((item: any) => item.fileId).filter(Boolean);
});

async function handleImageChange(file: UploadFile) {
  if (!file.raw) return;
  if (!file.raw.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件');
    return;
  }
  if (file.raw.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 10MB');
    return;
  }
  uploadingImage.value = true;
  try {
    const result = await userApi.uploadItemImage(file.raw);
    imageFileIds.value.push(result.fileId);
    imageUrls.value.push(result.fileUrl);
  } finally {
    uploadingImage.value = false;
  }
}

function removeImage(index: number) {
  imageFileIds.value.splice(index, 1);
  imageUrls.value.splice(index, 1);
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  if (!imageFileIds.value.length) {
    ElMessage.warning('请至少上传一张商品图片');
    return;
  }
  if (!form.contactPhone && !form.contactQq && !form.contactWechat) {
    ElMessage.warning('手机号、QQ、微信至少填写一项');
    return;
  }
  if (form.originalPrice && form.originalPrice < form.price) {
    ElMessage.warning('原价不能低于当前价格');
    return;
  }

  submitting.value = true;
  try {
    const payload = {
      ...form,
      imageFileIds: imageFileIds.value,
      coverImageFileId: imageFileIds.value[0]
    };
    if (isEdit.value) {
      await userApi.updateItem(Number(route.query.editId), payload);
      ElMessage.success('商品已更新');
    } else {
      await userApi.createItem(payload);
      ElMessage.success('商品已发布');
    }
    router.push('/user/my-items');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.panel {
  padding: 24px;
}

.publish-banner {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 18px;
  margin-bottom: 24px;
  padding: 18px 20px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(35, 78, 119, 0.08), rgba(220, 127, 57, 0.1));
}

.publish-banner strong {
  display: block;
  margin-bottom: 8px;
  font-size: 20px;
}

.publish-banner p {
  margin: 0;
  color: var(--text-soft);
}

.publish-tips {
  display: grid;
  gap: 10px;
  align-content: center;
}

.publish-tips span {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.56);
  color: var(--text-soft);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 18px;
}

.image-uploader {
  display: grid;
  gap: 14px;
}

.image-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.image-card {
  position: relative;
}

.image-list img {
  width: 104px;
  height: 104px;
  border-radius: 14px;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  right: 6px;
  top: 6px;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  cursor: pointer;
}

.cover-badge {
  position: absolute;
  left: 8px;
  bottom: 8px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(25, 63, 58, 0.8);
  color: #fff;
  font-size: 12px;
}

@media (max-width: 900px) {
  .publish-banner,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
