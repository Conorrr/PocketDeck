<script lang="ts">
	let { cardId, count = null, name, ...restProps } = $props();
	let isLoaded = $state(false);
</script>

<div {...restProps}>
	<div class="relative">
		{#if !isLoaded}
			<div class="absolute inset-0 bg-slate-300 animate-pulse" aria-hidden="true"></div>
		{/if}
		{#if cardId}
			<img
				src="/card-images/{cardId}.webp"
				width="367ox"
				height="512px"
				alt="{name} Card"
				onload={() => (isLoaded = true)}
			/>
		{/if}
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
