<script lang="ts">
	let { cardId, count = null, ...restProps } = $props();
</script>

<div {...restProps}>
	<div class="relative">
		<img src="/card-images/{cardId}.webp" width="367ox" height="512px"/>

		{#if count}
			<div
				class="
					absolute top-3 right-3
					inline-flex items-center justify-center
					px-3 py-1
					rounded-full
					bg-gray-700
					text-gray-200
					text-sm
					font-medium
					min-w-[1.5rem]
					opacity-80
					border
					border-white
				"
			>
				{count}×
			</div>
		{/if}
	</div>
</div>
