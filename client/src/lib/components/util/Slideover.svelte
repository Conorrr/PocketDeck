<script lang="ts">
	// key exists so that the scrollContainer will scroll to the top when it changes
	let { children, open = $bindable(), key = $bindable(null) } = $props();
	let scrollContainer: HTMLDivElement;
	$effect(() => {
		if (scrollContainer && key) {
			scrollContainer.scrollTo({ top: 0, behavior: 'auto' });
		}
	});

	function close() {
		open = false;
		key = null;
	}
</script>

<!-- Backdrop -->
<div
	class="fixed h-screen w-screen inset-0 bg-black/50 transition-opacity duration-300"
	class:opacity-0={!open}
	class:pointer-events-none={!open}
	onclick={close}
/>

<!-- Panel -->
<div
	class="fixed inset-y-0 right-0 w-full max-w-md flex-grow px-2 py-2 transform transition-transform duration-300 flex flex-col"
	class:translate-x-full={!open}
>
	<button
		class="z-2 absolute top-8 right-8 flex items-center justify-center h-8 w-8 rounded-full bg-gray-700 text-gray-200 hover:bg-gray-900 hover:text-gray-200 shadow-sm transition-colors"
		onclick={close}
	>
		✕
	</button>

	<div
		class="bg-slate-50 mx-4 my-4 flex-1 overflow-y-scroll shadow-xl bg-white rounded-2xl shadow"
		bind:this={scrollContainer}
	>
		{@render children?.()}
	</div>
</div>
