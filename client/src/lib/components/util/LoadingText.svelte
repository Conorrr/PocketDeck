<script lang="ts">
	let { value, width = "w-20" } = $props();
</script>

<span class="inline-flex flex-col items-start justify-items-start text-sm gap-2 text-gray-800 leading-none align-baseline">
	{#if value}
		{#if typeof value === "string"}
			<span class="text-gray-900">{value}</span>
		{:else}
			{#each value as rowValue}
				<span class="text-gray-900">{rowValue}</span>
			{/each}
		{/if}
	{:else}
		<span
			class="inline-block h-[1em] {width} translate-y-[3px] animate-pulse rounded-sm bg-gray-200"
		></span>
	{/if}
</span>
